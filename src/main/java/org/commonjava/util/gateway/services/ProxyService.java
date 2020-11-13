package org.commonjava.util.gateway.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ProxyService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Classifier classifier;

    public Uni<byte[]> doRequestBytes( String path, HttpServerRequest request )
    {
        WebClient client = classifier.getWebClient( path );

        return client.get( path )./*putHeaders( getHeaders( request ) ).*/send().onItem().transform( resp -> {
            logger.debug( "Get resp: {}, headers: {}", resp.statusMessage(), resp.headers() );
            if ( resp.statusCode() == 200 )
            {
                return resp.body().getBytes();
            }
            else
            {
                return null;
            }
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
