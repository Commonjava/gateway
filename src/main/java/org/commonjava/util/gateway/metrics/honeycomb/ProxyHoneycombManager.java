package org.commonjava.util.gateway.metrics.honeycomb;

import io.vertx.core.http.HttpServerRequest;
import org.commonjava.o11yphant.honeycomb.HoneycombManager;
import org.commonjava.o11yphant.honeycomb.SimpleTraceSampler;
import org.commonjava.util.gateway.config.ProxyHoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import static org.commonjava.o11yphant.metrics.RequestContextConstants.*;
import static org.commonjava.util.gateway.config.ProxyHoneycombConfiguration.ERROR_CLASS;
import static org.commonjava.util.gateway.config.ProxyHoneycombConfiguration.ERROR_MESSAGE;

@ApplicationScoped
public class ProxyHoneycombManager
                extends HoneycombManager
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    public ProxyHoneycombManager( ProxyHoneycombConfiguration honeycombConfiguration )
    {
        super( honeycombConfiguration, new SimpleTraceSampler( honeycombConfiguration ) );
    }

    @PostConstruct
    public void init()
    {
        super.init();
        registerRootSpanFields( new JvmRootSpanFields() );
    }

    public void addFields( long elapse, HttpServerRequest request, Object item, Throwable err )
    {
        configuration.getFieldSet().forEach( field -> {
            Object value = getContext( elapse, field, request, item, err );
            if ( value != null )
            {
                logger.trace( "Add field, {}={}", field, value );
                addSpanField( field, value );
            }
        } );
    }

    private Object getContext( long elapse, String field, HttpServerRequest request, Object item, Throwable err )
    {
        Response resp = null;
        if ( item instanceof Response )
        {
            resp = (Response) item;
        }

        Object ret = null;
        switch ( field )
        {
            case HTTP_METHOD:
                ret = request.rawMethod();
                break;
            case HTTP_STATUS:
                ret = ( resp != null ? resp.getStatus() : null );
                break;
            case TRACE_ID:
                ret = request.getHeader( EXTERNAL_ID );
                break;
            case CLIENT_ADDR:
                ret = request.remoteAddress().host();
                break;
            case REST_ENDPOINT_PATH:
                ret = request.path();
                break;
            case REQUEST_LATENCY_MILLIS:
                ret = elapse;
                break;
            case ERROR_MESSAGE:
                ret = ( err != null ? err.getMessage() : null );
                break;
            case ERROR_CLASS:
                ret = ( err != null ? err.getClass().getName() : null );
                break;
            default:
                break;
        }
        return ret;
    }

}
