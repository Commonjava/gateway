package org.commonjava.util.gateway.util;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.commonjava.util.gateway.config.ProxyConfiguration;
import org.commonjava.util.gateway.config.ServiceConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static javax.ws.rs.core.HttpHeaders.HOST;

public class WebClientAdapter
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private long DEFAULT_BACKOFF_MILLIS = Duration.ofSeconds( 5 ).toMillis();

    private final String PROXY_ORIGIN = "proxy-origin";

    private final ProxyConfiguration proxyConfiguration;

    private ServiceConfig serviceConfig;

    private AtomicLong timeout;

    private OtelAdapter otel;

    private OkHttpClient client;

    public WebClientAdapter( ServiceConfig serviceConfig, ProxyConfiguration proxyConfiguration, AtomicLong timeout,
                             OtelAdapter otel )
    {
        this.serviceConfig = serviceConfig;
        this.proxyConfiguration = proxyConfiguration;
        this.timeout = timeout;
        this.otel = otel;
        reinit();
    }

    public RequestAdapter head( String path, HttpServerRequest req )
    {
        return new RequestAdapter( new Request.Builder().head().url( calculateUrl(path) ), path ).headersFrom( req );
    }

    public RequestAdapter get( String path, HttpServerRequest req )
    {
        return new RequestAdapter( new Request.Builder().get().url( calculateUrl(path) ), path ).headersFrom( req );
    }

    public RequestAdapter post( String path, InputStream is, HttpServerRequest req )
    {
        try
        {
            File bodyFile = cacheInputStream( is );
            return new RequestAdapter(
                            new Request.Builder().post( RequestBody.create( bodyFile, getMediaType( req ) ) )
                                                 .url( calculateUrl( path ) ), path ).withCleanup(
                            new DeleteInterceptor( bodyFile ) ).headersFrom( req );
        }
        catch ( IOException exception )
        {
            return new RequestAdapter( exception );
        }
    }

    public RequestAdapter put( String path, InputStream is, HttpServerRequest req )
    {
        try
        {
            File bodyFile = cacheInputStream( is );
            return new RequestAdapter(
                            new Request.Builder().put( RequestBody.create( bodyFile, getMediaType( req ) ) )
                                                 .url( calculateUrl( path ) ), path ).withCleanup(
                            new DeleteInterceptor( bodyFile ) ).headersFrom( req );
        }
        catch ( IOException exception )
        {
            return new RequestAdapter( exception );
        }
    }

    private MediaType getMediaType( HttpServerRequest req )
    {
        String contentType = req.getHeader( "Content-Type" );
        if ( contentType != null )
        {
            return MediaType.get( contentType );
        }
        return null;
    }

    private File cacheInputStream( InputStream is ) throws IOException
    {
        File bodyFile = Files.createTempFile( "post-", ".bin" ).toFile();
        try (OutputStream os = new FileOutputStream( bodyFile ))
        {
            IOUtils.copy( is, os );
        }

        return bodyFile;
    }

    public RequestAdapter delete( String path )
    {
        return new RequestAdapter( new Request.Builder().delete().url( calculateUrl(path) ), path );
    }

    private String calculateUrl( String path )
    {
        StringBuilder sb = new StringBuilder("http");
        if ( serviceConfig.ssl )
        {
            sb.append('s');
        }
        sb.append("://").append(serviceConfig.host);
        if ( serviceConfig.port > 0 )
        {
            sb.append( ':' ).append( serviceConfig.port );
        }

        if ( !path.startsWith( "/" ) )
        {
            sb.append('/');
        }

        sb.append( path );

        return sb.toString();
    }

    public void reinit()
    {
        Duration d = Duration.ofMillis( timeout.get() );
        this.client = new OkHttpClient.Builder().addInterceptor( new RetryInterceptor( proxyConfiguration.getRetry() ) )
                                                .callTimeout( d )
                                                .readTimeout( d )
                                                .writeTimeout( d )
                                                .connectTimeout( d )
                                                .retryOnConnectionFailure( true )
                                                .build();
    }

    public final class RequestAdapter
    {
        private Request.Builder requestBuilder;

        private String path;

        private IOException exception;

        private Interceptor cleanupInterceptor;

        public RequestAdapter( Request.Builder requestBuilder, String path )
        {
            this.requestBuilder = requestBuilder;
            this.path = path;
        }

        public RequestAdapter( IOException exception )
        {
            this.exception = exception;
        }

        public RequestAdapter headersFrom( HttpServerRequest request )
        {
            if ( exception != null )
            {
                return this;
            }

            io.vertx.core.MultiMap headers = request.headers();
            headers.forEach( h -> {
                if ( !HOST.equalsIgnoreCase( h.getKey() ) )
                {
                    requestBuilder.header( h.getKey(), h.getValue() );
                }
            } );

            if ( headers.get( PROXY_ORIGIN ) == null )
            {
                String uri = request.absoluteURI();

                try
                {
                    URL url = new URL( uri );
                    String protocol = url.getProtocol();
                    String authority = url.getAuthority();
                    requestBuilder.header( PROXY_ORIGIN, String.format( "%s://%s", protocol, authority ) );
                }
                catch ( MalformedURLException e )
                {
                    logger.error( "Failed to parse requested URI: '" + uri + "'", e ); // shouldn't happen
                }
            }

            return this;
        }

        public CallAdapter call()
        {
            if ( exception != null )
            {
                return new CallAdapter( exception );
            }

            Duration pathTimeout = serviceConfig.getMappedTimeout( path );
            if ( otel.enabled() )
            {
                Span.current()
                    .setAttribute( "target.timeout",
                                   pathTimeout != null ? pathTimeout.toMillis() : timeout.get() );
            }

            OkHttpClient callClient = client;
            if ( pathTimeout != null || cleanupInterceptor != null )
            {
                OkHttpClient.Builder builder = client.newBuilder();
                if ( pathTimeout != null )
                {
                    builder.callTimeout( pathTimeout )
                           .readTimeout( pathTimeout )
                           .writeTimeout( pathTimeout )
                           .connectTimeout( pathTimeout );
                }

                if ( cleanupInterceptor != null )
                {
                    builder.interceptors().add( 0, cleanupInterceptor );
//                    builder.addInterceptor( cleanupInterceptor );
                }

                callClient = builder.build();
            }

            return new CallAdapter( callClient, requestBuilder, serviceConfig );
        }

        public RequestAdapter withCleanup( Interceptor cleanupInterceptor )
        {
            this.cleanupInterceptor = cleanupInterceptor;
            return this;
        }
    }

    public final class CallAdapter
    {
        private OkHttpClient callClient;

        private Request.Builder requestBuilder;

        private ServiceConfig serviceConfig;

        private IOException exception;

        public CallAdapter( IOException exception )
        {
            this.exception = exception;
        }

        public CallAdapter( OkHttpClient callClient, Request.Builder requestBuilder, ServiceConfig serviceConfig )
        {
            this.callClient = callClient;
            this.requestBuilder = requestBuilder;
            this.serviceConfig = serviceConfig;
        }

        public Uni<Response> enqueue()
        {
            if ( exception != null )
            {
                return UniHelper.toUni( Future.failedFuture( exception ) );
            }

            return UniHelper.toUni( Future.future( ( p ) -> {
                logger.debug( "Starting upstream request..." );

                Span span;
                Scope scope;
                if ( otel.enabled() )
                {
                    span = otel.newClientSpan( "okhttp",
                                                    requestBuilder.build().method() + ":" + serviceConfig.host + ":"
                                                                    + serviceConfig.port );

                    scope = span.makeCurrent();

                    otel.injectContext( requestBuilder );
                }
                else
                {
                    span = null;
                    scope = null;
                }

                Call call = callClient.newCall( requestBuilder.build() );

                if ( span != null )
                {
                    span.setAttribute( SemanticAttributes.HTTP_METHOD, call.request().method() );
                    span.setAttribute( SemanticAttributes.HTTP_HOST, call.request().url().host() );
                    span.setAttribute( SemanticAttributes.HTTP_URL, call.request().url().url().toExternalForm() );
                }

                call.enqueue( new Callback()
                {
                    @Override
                    public void onFailure( @NotNull Call call, @NotNull IOException e )
                    {
                        if ( span != null )
                        {
                            span.setAttribute( "error.class", e.getClass().getSimpleName() );
                            span.setAttribute( "error.message", e.getMessage() );

                            // NOTE: Because we're doing this using a Future, we can't use try-with-resources/finally, as
                            // the OTEL example shows.
                            scope.close();
                            span.end();
                        }

                        logger.trace( "Failed: " + call.request().url(), e );
                        p.fail( e );
                    }

                    @Override
                    public void onResponse( @NotNull Call call, @NotNull Response response )
                    {
                        if ( span != null )
                        {
                            span.setAttribute( SemanticAttributes.HTTP_STATUS_CODE, response.code() );
                            span.setAttribute( SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH.getKey(),
                                               response.header( "Content-Length" ) );

                            // NOTE: Because we're doing this using a Future, we can't use try-with-resources/finally, as
                            // the OTEL example shows.
                            scope.close();
                            span.end();
                        }

                        logger.trace( "Success: " + call.request().url() + " -> " + response.code() );
                        p.complete( response );
                    }
                } );
            } ) );
        }

    }

    private class RetryInterceptor
                    implements Interceptor
    {
        private final int count;

        private final long interval;

        RetryInterceptor( ProxyConfiguration.Retry retry )
        {
            this.count = retry == null || retry.count < 0 ? 3 : retry.count;
            this.interval = retry == null || retry.interval < 10 ? DEFAULT_BACKOFF_MILLIS : retry.interval;
        }

        @NotNull
        @Override
        public Response intercept( @NotNull Chain chain ) throws IOException
        {
            Request req = chain.request();
            Response resp = null;
            int tryCounter = 0;
            do {
                if ( resp != null )
                {
                    // We will get java.lang.IllegalStateException if the previous response is still open for
                    // whatever reason when retrying
                    resp.close();
                }
                try
                {
                    resp = chain.proceed( req );
                    if ( resp.code() < 500 )
                    {
                        return resp;
                    }
                    else
                    {
                        if ( tryCounter >= count )
                        {
                            return resp;
                        }
                        else
                        {
                            if ( otel.enabled() )
                            {
                                Span.current()
                                    .setAttribute( "target.try." + tryCounter + ".status_code", resp.code() );
                            }

                            logger.debug( "TRY({}/{}): Response missing or indicates server error: {}. Retrying",
                                          tryCounter, count, resp );
                        }
                    }
                }
                catch( IOException e )
                {
                    if ( tryCounter >= count )
                    {
                        throw e;
                    }

                    if ( otel.enabled() )
                    {
                        Span.current().setAttribute( "target.try." + tryCounter + ".error_message", e.getMessage() );
                        Span.current()
                            .setAttribute( "target.try." + tryCounter + ".error_class", e.getClass().getSimpleName() );
                    }

                    logger.debug( "TRY(" + tryCounter + "/" + count + "): Failed upstream request: " + req.url(), e );
                }

                try
                {
                    Thread.sleep( interval );
                }
                catch ( InterruptedException e )
                {
                    if ( otel.enabled() )
                    {
                        Span.current().setAttribute( "target.interrupted", 1 );
                        Span.current().setAttribute( "target.try." + tryCounter + ".interrupted", 1 );
                    }

                    return new Response.Builder().code( 502 ).message( "Thread interruption while waiting for upstream retry!" ).build();
                }

                tryCounter++;
            }
            while ( tryCounter < count );

            if ( otel.enabled() )
            {
                Span.current().setAttribute( "target.retries", tryCounter );
            }

            throw new IOException( "Proxy retry interceptor reached an unexpected fall-through condition!" );
        }
    }

    private class DeleteInterceptor implements Interceptor
    {
        private final File bodyFile;

        public DeleteInterceptor( File bodyFile )
        {
            this.bodyFile = bodyFile;
        }

        @NotNull
        @Override
        public Response intercept( @NotNull Chain chain ) throws IOException
        {
            try
            {
                if ( otel.enabled() )
                {
                    Span.current().setAttribute( "gateway.target.bodyFile", bodyFile.getPath() );
                }

                return chain.proceed( chain.request() );
            }
            finally
            {
                logger.debug( "Deleting input post/put body tempfile: {}", bodyFile );
                boolean deleted = Files.deleteIfExists( bodyFile.toPath() );
                if ( !deleted )
                {
                    logger.trace( "Could not delete body input file: {}", bodyFile );
                }
            }
        }
    }
}
