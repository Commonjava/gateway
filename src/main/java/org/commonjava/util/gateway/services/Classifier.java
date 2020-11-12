package org.commonjava.util.gateway.services;

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

    @ConfigProperty( name = "default.host" )
    private String defaultHost;

    @PostConstruct
    void initialize()
    {
        this.defaultClient = WebClient.create( vertx, new WebClientOptions().setDefaultHost( defaultHost )
                                                                            .setDefaultPort( 80 ) );
    }

    public WebClient getWebClient( String path )
    {
        if ( path.startsWith( "promote" ) )
        {
            //return promoteClient; // set to different services
        }
        return defaultClient;
    }

}

