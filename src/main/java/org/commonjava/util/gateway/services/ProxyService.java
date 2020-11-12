package org.commonjava.util.gateway.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProxyService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public Uni<String> doRequest( WebClient client, String path, HttpServerRequest request )
    {
        return client.get( path ).putHeaders( getHeaders( request ) ).send().onItem().transform( resp -> {
            logger.debug("Get resp: {}", resp.statusMessage() );
            if ( resp.statusCode() == 200 )
            {
                return resp.bodyAsString();
            }
            else
            {
                return getErrorMessage( resp );
            }
        } );
    }


    private String getErrorMessage( HttpResponse<Buffer> resp )
    {
        return new JsonObject().put( "code", resp.statusCode() ).put( "message", resp.bodyAsString() ).toString();
    }

    private io.vertx.mutiny.core.MultiMap getHeaders( HttpServerRequest request )
    {
        MultiMap headers = request.headers();
        //headers.names().forEach( name -> System.out.println( ">>> " + name + ": " + headers.getAll( name ) ) );
        return io.vertx.mutiny.core.MultiMap.newInstance( headers ).remove( "Host" );
    }

}
