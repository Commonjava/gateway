/**
 * Copyright (C) 2022 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.util.gateway.util;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
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

    public Span newClientSpan( String adapterName, String name )
    {
        return getOtelInstance().getTracer( adapterName ).spanBuilder( name )
                                       .setSpanKind( SpanKind.CLIENT )
                                       .setAttribute( "service_name", "gateway" )
                                       .startSpan();
    }

    public void injectContext( Request.Builder requestBuilder )
    {
        getOtelInstance().getPropagators()
                .getTextMapPropagator()
                .inject( Context.current(), requestBuilder, OKHTTP_CONTEXT_SETTER );

    }

    private OpenTelemetry getOtelInstance() {
        OpenTelemetry otelInstance;
        if ( !enabled )
        {
            otelInstance = OpenTelemetry.noop();
        } else {
            otelInstance = GlobalOpenTelemetry.get();
        }
        return otelInstance;
    }
}
