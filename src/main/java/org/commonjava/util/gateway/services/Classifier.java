package org.commonjava.util.gateway.services;

import io.quarkus.runtime.Startup;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.commonjava.util.gateway.config.ProxyServiceConfiguration;
import org.commonjava.util.gateway.exception.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Startup
@ApplicationScoped
public class Classifier
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    Vertx vertx;

    @Inject
    private ProxyServiceConfiguration serviceConfiguration;

    @PostConstruct
    void init()
    {
        serviceConfiguration.services.forEach(
                        service -> logger.info( "Proxy service, host: {}, port: {}, methods: {}, pathPattern: {}",
                                                service.host, service.port, service.methods, service.pathPattern ) );
    }

    private Map<ProxyServiceConfiguration.ServiceConfig, WebClient> clientMap = new ConcurrentHashMap<>();

    public <R> R classifyAnd( String path, HttpServerRequest request, Function<WebClient, R> action ) throws Exception
    {
        return action.apply( getWebClient( path, request ) );
    }

    private WebClient getWebClient( String path, HttpServerRequest request ) throws Exception
    {
        HttpMethod method = request.method();

        ProxyServiceConfiguration.ServiceConfig service = null;
        for ( ProxyServiceConfiguration.ServiceConfig sv : serviceConfiguration.services )
        {
            if ( path.matches( sv.pathPattern ) && ( sv.methods == null || sv.methods.contains( method.name() ) ) )
            {
                service = sv;
                break;
            }
        }

        if ( service != null )
        {
            return clientMap.computeIfAbsent( service,
                                              k -> WebClient.create( vertx,
                                                       new WebClientOptions()
                                                             .setDefaultHost( k.host )
                                                                .setDefaultPort( k.port ) ) );
        }
        else
        {
            throw new ServiceNotFoundException( "Service not found, path: " + path + ", method: " + method );
        }
    }
}

