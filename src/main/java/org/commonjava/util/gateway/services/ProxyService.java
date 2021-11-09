package org.commonjava.util.gateway.services;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.commonjava.util.gateway.cache.CacheHandler;
import org.commonjava.util.gateway.config.ProxyConfiguration;
import org.commonjava.util.gateway.interceptor.ExceptionHandler;
import org.commonjava.util.gateway.interceptor.MetricsHandler;
import org.commonjava.util.gateway.util.BufferStreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.vertx.core.http.HttpMethod.HEAD;
import static io.vertx.core.http.impl.HttpUtils.normalizePath;
import static javax.ws.rs.core.HttpHeaders.HOST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.EXTERNAL_ID;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.TRACE_ID;
import static org.commonjava.util.gateway.services.ProxyConstants.EVENT_PROXY_CONFIG_CHANGE;
import static org.commonjava.util.gateway.services.ProxyConstants.FORBIDDEN_HEADERS;
import static org.commonjava.util.gateway.util.ServiceUtils.getTimeout;
import static org.commonjava.util.gateway.util.ServiceUtils.parseTimeout;

@ApplicationScoped
@MetricsHandler
@ExceptionHandler
public class ProxyService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis( 5 );

    private long DEFAULT_BACKOFF_MILLIS = Duration.ofSeconds( 5 ).toMillis();

    private final String PROXY_ORIGIN = "proxy-origin";

    private volatile long timeout;

    @Inject
    ProxyConfiguration proxyConfiguration;

    @Inject
    Classifier classifier;

    @Inject
    CacheHandler cacheHandler;

    @PostConstruct
    void init()
    {
        timeout = readTimeout();
        logger.debug( "Init, timeout: {}", timeout );
    }

    private long readTimeout()
    {
        long t = DEFAULT_TIMEOUT;
        String readTimeout = proxyConfiguration.getReadTimeout();
        if ( isNotBlank( readTimeout ) )
        {
            try
            {
                t = parseTimeout( readTimeout );
            }
            catch ( Exception e )
            {
                logger.error( "Failed to parse proxy.read-timeout, use default " + DEFAULT_TIMEOUT, e );
            }
        }
        return t;
    }

    @ConsumeEvent( value = EVENT_PROXY_CONFIG_CHANGE )
    void handleConfigChange( String message )
    {
        timeout = readTimeout();
        logger.debug( "Handle event {}, refresh timeout: {}", EVENT_PROXY_CONFIG_CHANGE, timeout );
    }

    public Uni<Response> doHead( String path, HttpServerRequest request ) throws Exception
    {
        return normalizePathAnd( path, p -> classifier.classifyAnd( p, request,
                                                                    (client, service) -> wrapAsyncCall( client.head( p )
                                                                                                   .putHeaders( getHeaders( request ) )
                                                                                                   .timeout( getTimeout( service, p, timeout ) )
                                                                                                   .send(), request.method() ) ) );
    }

    public Uni<Response> doGet( String path, HttpServerRequest request ) throws Exception
    {
        return normalizePathAnd( path, p -> classifier.classifyAnd( p, request,
                                                                    (client, service) ->
                                                                                    cacheHandler.wrapWithCache( wrapAsyncCall( client.get( p )
                                                                                                   .putHeaders( getHeaders( request ) )
                                                                                                   .timeout( getTimeout( service, p, timeout ) )
                                                                                                   .send(), request.method() ), p, service ) ) );
    }

    public Uni<Response> doPost( String path, InputStream is, HttpServerRequest request ) throws Exception
    {
        Buffer buf = Buffer.buffer( IOUtils.toByteArray( is ) );

        return normalizePathAnd( path, p -> classifier.classifyAnd( p, request,
                                                                    (client, service) -> wrapAsyncCall( client.post( p )
                                                                                                   .putHeaders( getHeaders( request ) )
                                                                                                   .timeout( getTimeout( service, p, timeout ) )
                                                                                                   .sendBuffer( buf ), request.method() ) ) );
    }

    public Uni<Response> doPut( String path, InputStream is, HttpServerRequest request ) throws Exception
    {
        Buffer buf = Buffer.buffer( IOUtils.toByteArray( is ) );

        return normalizePathAnd( path, p -> classifier.classifyAnd( p, request,
                                                                    (client, service) -> wrapAsyncCall( client.put( p )
                                                                                                   .putHeaders( getHeaders( request ) )
                                                                                                   .timeout( getTimeout( service, p, timeout ) )
                                                                                                   .sendBuffer( buf ), request.method() ) ) );
    }

    public Uni<Response> doDelete( String path, HttpServerRequest request ) throws Exception
    {
        return normalizePathAnd( path, p -> classifier.classifyAnd( p, request,
                                                                    (client, service) -> wrapAsyncCall( client.delete( p )
                                                                                                   .putHeaders( getHeaders( request ) )
                                                                                                   .timeout( getTimeout( service, p, timeout ) )
                                                                                                   .send(), request.method() ) ) );
    }

    private Uni<Response> wrapAsyncCall( Uni<HttpResponse<Buffer>> asyncCall, HttpMethod method )
    {
        ProxyConfiguration.Retry retry = proxyConfiguration.getRetry();
        Uni<Response> ret = asyncCall.onItem().transform( ( HttpResponse<Buffer> resp ) -> convertProxyResp( resp, method ) );
        if ( retry.count > 0 )
        {
            long backOff = retry.interval;
            if ( retry.interval <= 0 )
            {
                backOff = DEFAULT_BACKOFF_MILLIS;
            }
            ret = ret.onFailure( t -> ( t instanceof IOException || t instanceof VertxException ) )
               .retry()
               .withBackOff( Duration.ofMillis( backOff ) )
               .atMost( retry.count );
        }
        return ret.onFailure().recoverWithItem( this::handleProxyException );
    }

    /**
     * Send status 500 with error message body.
     * @param t error
     */
    private Response handleProxyException( Throwable t )
    {
        logger.error( "Proxy error", t );
        return Response.status( INTERNAL_SERVER_ERROR ).entity( t + ". Caused by: " + t.getCause() ).build();
    }

    /**
     * Read status and headers from proxy resp and set them to direct response.
     * @param resp proxy resp
     * @param method request method
     */
    private Response convertProxyResp( HttpResponse<Buffer> resp, HttpMethod method )
    {
        logger.debug( "Proxy resp: {} {}", resp.statusCode(), resp.statusMessage() );
        logger.trace( "Raw resp headers:\n{}", resp.headers() );
        Response.ResponseBuilder builder = Response.status( resp.statusCode(), resp.statusMessage() );
        resp.headers().forEach( header -> {
            if ( isHeaderAllowed( header, method ) )
            {
                builder.header( header.getKey(), header.getValue() );
            }
        } );
        if ( resp.body() != null )
        {
            StreamingOutput so = new BufferStreamingOutput( resp );
            builder.entity( so );
        }
        return builder.build();
    }

    /**
     * Raw content-length/connection header breaks http2 protocol. Exclude them and let lower layer regenerate it.
     * Allow all headers when it is HEAD request.
     */
    private boolean isHeaderAllowed( Map.Entry<String, String> header, HttpMethod method )
    {
        if ( method == HEAD )
        {
            return true;
        }
        String key = header.getKey();
        return !FORBIDDEN_HEADERS.contains( key.toLowerCase() );
    }

    private io.vertx.mutiny.core.MultiMap getHeaders( HttpServerRequest request )
    {
        MultiMap headers = request.headers();
        io.vertx.mutiny.core.MultiMap ret = io.vertx.mutiny.core.MultiMap.newInstance( headers )
                                                                         .remove( HOST )
                                                                         .add( TRACE_ID, getTraceId( headers ) );
        if ( headers.get( PROXY_ORIGIN ) == null )
        {
            try
            {
                URL url = new URL( request.absoluteURI() );
                String protocol = url.getProtocol();
                String authority = url.getAuthority();
                ret.add( PROXY_ORIGIN, String.format( "%s://%s", protocol, authority ) );
            }
            catch ( MalformedURLException e )
            {
                logger.error( "Failed to parse URI", e ); // shouldn't happen
            }
        }

        logger.trace( "Req headers:\n{}", ret );
        return ret;
    }

    /**
     * Get 'trace-id'. If client specify an 'external-id', use it. Otherwise, use an generated uuid. Services under the hook
     * should use the hereby created 'trace-id', rather than to generate their own.
     */
    private String getTraceId( MultiMap headers )
    {
        String externalID = headers.get( EXTERNAL_ID );
        return isNotBlank( externalID ) ? externalID : UUID.randomUUID().toString();
    }

    @FunctionalInterface
    private interface CheckedFunction<T, R>
    {
        R apply( T t ) throws Exception;
    }

    private <R> R normalizePathAnd( String path, CheckedFunction<String, R> action ) throws Exception
    {
        return action.apply( normalizePath( path ) );
    }

}
