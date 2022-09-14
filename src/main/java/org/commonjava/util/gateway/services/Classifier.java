package org.commonjava.util.gateway.services;

import io.opentelemetry.api.trace.Span;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import org.apache.commons.io.FilenameUtils;
import org.commonjava.util.gateway.config.ProxyConfiguration;
import org.commonjava.util.gateway.config.ServiceConfig;
import org.commonjava.util.gateway.exception.ServiceNotFoundException;
import org.commonjava.util.gateway.util.OtelAdapter;
import org.commonjava.util.gateway.util.WebClientAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.util.gateway.services.ProxyConstants.EVENT_PROXY_CONFIG_CHANGE;
import static org.commonjava.util.gateway.util.ServiceUtils.parseTimeout;

@ApplicationScoped
public class Classifier
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final long DEFAULT_TIMEOUT = TimeUnit.MINUTES.toMillis( 5 );

    private final AtomicLong timeout = new AtomicLong( DEFAULT_TIMEOUT );

    @Inject
    ProxyConfiguration proxyConfiguration;

    @Inject
    OtelAdapter otel;

    private Map<ServiceConfig, WebClientAdapter> clientMap = new ConcurrentHashMap<>();

    @PostConstruct
    void init()
    {
        readTimeout();
        logger.debug( "Init, timeout: {}", timeout );
    }

    @ConsumeEvent( value = EVENT_PROXY_CONFIG_CHANGE )
    void handleConfigChange( String message )
    {
        readTimeout();
        clientMap.forEach( ( k, client ) -> client.reinit() );
        logger.debug( "Handle event {}, refresh timeout: {}", EVENT_PROXY_CONFIG_CHANGE, timeout );
    }

    private void readTimeout()
    {
        long t = DEFAULT_TIMEOUT;
        String readTimeout = proxyConfiguration.getReadTimeout();
        if ( isNotBlank( readTimeout ) )
        {
            try
            {
                t = parseTimeout( readTimeout );
            }
            catch ( Exception e )
            {
                logger.error( "Failed to parse proxy.read-timeout, use default " + DEFAULT_TIMEOUT, e );
            }
        }
        timeout.set( t );
    }

    public <R> R classifyAnd( String path, HttpServerRequest request,
                              BiFunction<WebClientAdapter, ServiceConfig, R> action ) throws Exception
    {
        Span span = Span.current();
        span.setAttribute( "service_name", "gateway" );
        span.setAttribute( "name", request.method().name() );
        span.setAttribute( "path.ext", FilenameUtils.getExtension( path ) );

        ServiceConfig service = getServiceConfig( path, request );
        if ( service == null )
        {
            span.setAttribute( "serviced", 0 );
            span.setAttribute( "missing.path", path );
            span.setAttribute( "missing.method", request.method().name() );

            throw new ServiceNotFoundException( "Service not found, path: " + path + ", method: " + request.method() );
        }

        span.setAttribute( "serviced", 1 );
        span.setAttribute( "target.host", service.host );
        span.setAttribute( "target.port", service.port );
        span.setAttribute( "target.method", request.method().name() );
        span.setAttribute( "target.path", path );

        return action.apply( getWebClient( service ), service );
    }

    private ServiceConfig getServiceConfig( String path, HttpServerRequest request ) throws Exception
    {
        HttpMethod method = request.method();

        ServiceConfig service = null;

        Set<ServiceConfig> services = proxyConfiguration.getServices();
        if ( services != null )
        {
            for ( ServiceConfig sv : services )
            {
                if ( path.matches( sv.pathPattern ) && ( sv.methods == null || sv.methods.contains( method.name() ) ) )
                {
                    service = sv;
                    break;
                }
            }
        }
        return service;
    }

    private WebClientAdapter getWebClient( ServiceConfig service ) throws Exception
    {
        return clientMap.computeIfAbsent( service, sc -> new WebClientAdapter( sc, proxyConfiguration, timeout, otel ) );
    }
}
