package org.commonjava.util.gateway.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class ProxyService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Classifier classifier;

    public void doHead( String path, HttpServerRequest request, HttpServerResponse response )
    {
        classifier.classifyAnd( path, request, ( client ) -> {
            HttpResponse<Buffer> resp = client.head( path ).sendAndAwait();
            logger.debug( "Head resp: {}, headers: {}", statusLine( resp ), resp.headers() );
            flushProxyResp( resp, response );
            response.end();
            return null;
        } );
    }

    public Uni<byte[]> doGetBytes( String path, HttpServerRequest request, HttpServerResponse response )
    {
        return classifier.classifyAnd( path, request, ( client ) -> {
            return client.get( path ).send().onItem().transform( resp -> {
                logger.debug( "Get resp: {}, headers: {}", statusLine( resp ), resp.headers() );
                flushProxyResp( resp, response );
                if ( resp.body() != null )
                {
                    return resp.body().getBytes();
                }
                return null;
            } );
        } );
    }

    public Uni<byte[]> doPostBytes( String path, InputStream is, HttpServerRequest request,
                                    HttpServerResponse response ) throws IOException
    {
        Buffer buf = Buffer.buffer( IOUtils.toByteArray( is ) );

        return classifier.classifyAnd( path, request, ( client ) -> {
            return client.post( path ).sendBuffer( buf ).onItem().transform( resp -> {
                logger.debug( "Post resp: {}, headers: {}", statusLine( resp ), resp.headers() );
                flushProxyResp( resp, response );
                if ( resp.body() != null )
                {
                    return resp.body().getBytes();
                }
                return null;
            } );
        } );
    }

    public Uni<byte[]> doPutBytes( String path, InputStream is, HttpServerRequest request, HttpServerResponse response )
                    throws IOException
    {
        Buffer buf = Buffer.buffer( IOUtils.toByteArray( is ) );

        return classifier.classifyAnd( path, request, ( client ) -> {
            return client.put( path ).sendBuffer( buf ).onItem().transform( resp -> {
                logger.debug( "Put resp: {}, headers: {}", statusLine( resp ), resp.headers() );
                flushProxyResp( resp, response );
                if ( resp.body() != null )
                {
                    return resp.body().getBytes();
                }
                return null;
            } );
        } );
    }

    /**
     * Read status and headers from proxy resp and set them to direct response.
     * @param resp proxy resp
     * @param response direct response
     */
    private void flushProxyResp( HttpResponse<Buffer> resp, HttpServerResponse response )
    {
        resp.headers().forEach( header -> response.putHeader( header.getKey(), header.getValue() ) );
        response.setStatusCode( resp.statusCode() ).setStatusMessage( resp.statusMessage() );
    }

    private String statusLine( HttpResponse<Buffer> resp )
    {
        return resp.statusCode() + " " + resp.statusMessage();
    }

/*
    private io.vertx.mutiny.core.MultiMap getHeaders( HttpServerRequest request )
    {
        MultiMap headers = request.headers();
        //headers.names().forEach( name -> System.out.println( ">>> " + name + ": " + headers.getAll( name ) ) );
        return io.vertx.mutiny.core.MultiMap.newInstance( headers ).remove( "Host" );
    }
*/

}
