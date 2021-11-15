package org.commonjava.util.gateway.metrics.jfr;

import io.quarkus.runtime.StartupEvent;
import jdk.jfr.FlightRecorder;
import org.commonjava.util.gateway.metrics.jfr.events.JaxRSEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class FlightRecorderMetrics
{
    public void registerEvents( @Observes StartupEvent e )
    {
        FlightRecorder.register( JaxRSEvent.class );
    }
}
