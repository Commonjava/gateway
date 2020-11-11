package org.commonjava.util.gateway.services;

import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class Classifier
{
    @Inject
    Vertx vertx;

    private WebClient defaultClient;

    private final String targetHost = "indy-infra-nos-automation.cloud.paas.psi.redhat.com";

    @PostConstruct
    void initialize()
    {
        this.defaultClient = WebClient.create( vertx, new WebClientOptions().setDefaultHost( targetHost )
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

