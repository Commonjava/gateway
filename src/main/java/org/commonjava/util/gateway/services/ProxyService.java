package org.commonjava.util.gateway.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.commonjava.util.gateway.interceptor.ProxyExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.UUID;

import static javax.ws.rs.core.HttpHeaders.HOST;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.util.gateway.GatewayConstants.EXTERNAL_ID;
import static org.commonjava.util.gateway.GatewayConstants.TRACE_ID;

@ApplicationScoped
@ProxyExceptionHandler
public class ProxyService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    Classifier classifier;

    public Uni<Response> doHead( String path, HttpServerRequest request ) throws Exception
    {
        return classifier.classifyAnd( path, request, client -> client.head( path )
                                                                      .putHeaders( getHeaders( request ) )
                                                                      .send()
                                                                      .onItem()
                                                                      .transform( this::convertProxyResp ) );
    }

    public Uni<Response> doGet( String path, HttpServerRequest request ) throws Exception
    {
        return classifier.classifyAnd( path, request, client -> client.get( path )
                                                                      .putHeaders( getHeaders( request ) )
                                                                      .send()
                                                                      .onItem()
                                                                      .transform( this::convertProxyResp ) );
    }

    public Uni<Response> doPost( String path, InputStream is, HttpServerRequest request ) throws Exception
    {
        Buffer buf = Buffer.buffer( IOUtils.toByteArray( is ) );

        return classifier.classifyAnd( path, request, client -> client.post( path )
                                                                      .putHeaders( getHeaders( request ) )
                                                                      .sendBuffer( buf )
                                                                      .onItem()
                                                                      .transform( this::convertProxyResp ) );
    }

    public Uni<Response> doPut( String path, InputStream is, HttpServerRequest request ) throws Exception
    {
        Buffer buf = Buffer.buffer( IOUtils.toByteArray( is ) );

        return classifier.classifyAnd( path, request, client -> client.put( path )
                                                                      .putHeaders( getHeaders( request ) )
                                                                      .sendBuffer( buf )
                                                                      .onItem()
                                                                      .transform( this::convertProxyResp ) );
    }

    public Uni<Response> doDelete( String path, HttpServerRequest request ) throws Exception
    {
        return classifier.classifyAnd( path, request, client -> client.delete( path )
                                                                      .putHeaders( getHeaders( request ) )
                                                                      .send()
                                                                      .onItem()
                                                                      .transform( this::convertProxyResp ) );
    }

    /**
     * Read status and headers from proxy resp and set them to direct response.
     * @param resp proxy resp
     */
    private Response convertProxyResp( HttpResponse<Buffer> resp )
    {
        logger.debug( "Proxy resp: {} {}, resp headers:\n{}", resp.statusCode(), resp.statusMessage(), resp.headers() );
        Response.ResponseBuilder builder = Response.status( resp.statusCode(), resp.statusMessage() );
        resp.headers().forEach( header -> builder.header( header.getKey(), header.getValue() ) );
        if ( resp.body() != null )
        {
            byte[] bytes = resp.body().getBytes();
            builder.entity( bytes );
        }
        return builder.build();
    }

    private io.vertx.mutiny.core.MultiMap getHeaders( HttpServerRequest request )
    {
        MultiMap headers = request.headers();
        io.vertx.mutiny.core.MultiMap ret = io.vertx.mutiny.core.MultiMap.newInstance( headers )
                                                                         .remove( HOST )
                                                                         .add( TRACE_ID, getTraceId( headers ) );
        logger.debug( "Req headers:\n{}", ret );
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

}
