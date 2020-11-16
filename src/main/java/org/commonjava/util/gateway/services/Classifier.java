package org.commonjava.util.gateway.services;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class Classifier
{
    @Inject
    Vertx vertx;

    private WebClient defaultClient;

    private WebClient localClient; // for test PUT and POST

    @ConfigProperty( name = "default.host" )
    private String defaultHost;

    @PostConstruct
    void initialize()
    {
        this.defaultClient = WebClient.create( vertx, new WebClientOptions().setDefaultHost( defaultHost )
                                                                            .setDefaultPort( 80 ) );

        this.localClient = WebClient.create( vertx, new WebClientOptions().setDefaultHost( "localhost" )
                                                                          .setDefaultPort( 8080 ) );
    }

    public WebClient getWebClient( String path, HttpMethod method )
    {
        if ( method == HttpMethod.POST || method == HttpMethod.PUT )
        {
            return localClient;
        }
/*
        if ( path.startsWith( "/api/promote" ) )
        {
            return promoteClient; // set to different services
        }
*/
        return defaultClient;
    }

}

