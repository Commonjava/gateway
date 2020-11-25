package org.commonjava.util.gateway.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static javax.ws.rs.core.HttpHeaders.HOST;

@ApplicationScoped
public class ProxyService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Classifier classifier;

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
        logger.debug( "Proxy resp: {} {}, resp headers: {}", resp.statusCode(), resp.statusMessage(), resp.headers() );
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
        io.vertx.mutiny.core.MultiMap ret = io.vertx.mutiny.core.MultiMap.newInstance( headers ).remove( HOST );
        logger.debug( "Req headers: {}", ret );
        return ret;
    }

}
