package org.commonjava.util.gateway.util;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import okhttp3.Request;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OtelAdapter
{
    private static final TextMapSetter<? super Request.Builder> OKHTTP_CONTEXT_SETTER = ( rb, key, value ) -> {
        rb.header( key, value );
    };

    @ConfigProperty( name = "quarkus.opentelemetry.enabled" )
    Boolean enabled;

    public boolean enabled()
    {
        return enabled == Boolean.TRUE;
    }

    public Span newClientSpan( String adapterName, String name )
    {
        if ( !enabled )
        {
            return null;
        }

        return GlobalOpenTelemetry.get().getTracer( adapterName ).spanBuilder( name )
                                       .setSpanKind( SpanKind.CLIENT )
                                       .setAttribute( "service_name", "gateway" )
                                       .startSpan();
    }

    public void injectContext( Request.Builder requestBuilder )
    {
        if ( !enabled )
        {
            return;
        }

        GlobalOpenTelemetry.get().getPropagators()
                .getTextMapPropagator()
                .inject( Context.current(), requestBuilder, OKHTTP_CONTEXT_SETTER );

    }
}
