package org.commonjava.util.gateway.metrics.jfr;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.commonjava.util.gateway.metrics.jfr.events.JaxRSEvent;

import io.quarkus.runtime.StartupEvent;
import jdk.jfr.FlightRecorder;

@ApplicationScoped
public class FlightRecorderMetrics
{
    public void registerEvents( @Observes StartupEvent e )
    {
        FlightRecorder.register( JaxRSEvent.class );
    }
}
