package org.commonjava.util.gateway.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;
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

    public Uni<byte[]> doRequestBytes( String path, HttpServerRequest request )
    {
        WebClient client = classifier.getWebClient( path, request.method() );

        return client.get( path )./*putHeaders( getHeaders( request ) ).*/send().onItem().transform( resp -> {
            logger.debug( "Get resp: {}, headers: {}", resp.statusMessage(), resp.headers() );
            if ( resp.body() != null )
            {
                return resp.body().getBytes();
            }
            return null;
        } );
    }

    public Uni<byte[]> doPostBytes( String path, InputStream is, HttpServerRequest request ) throws IOException
    {
        WebClient client = classifier.getWebClient( path, request.method() );

        Buffer buf = Buffer.buffer( IOUtils.toByteArray( is ) );
        return client.post( path ).sendBuffer( buf ).onItem().transform( resp -> {
            logger.debug( "Post resp: {}, headers: {}", resp.statusMessage(), resp.headers() );
            if ( resp.body() != null )
            {
                return resp.body().getBytes();
            }
            return null;
        } );
    }

    public Uni<byte[]> doPutBytes( String path, InputStream is, HttpServerRequest request ) throws IOException
    {
        WebClient client = classifier.getWebClient( path, request.method() );

        Buffer buf = Buffer.buffer( IOUtils.toByteArray( is ) );
        return client.put( path ).sendBuffer( buf ).onItem().transform( resp -> {
            logger.debug( "Put resp: {}, headers: {}", resp.statusMessage(), resp.headers() );
            if ( resp.body() != null )
            {
                return resp.body().getBytes();
            }
            return null;
        } );
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
