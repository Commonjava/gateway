package org.commonjava.util.gateway.metrics.health;

import org.commonjava.util.gateway.metrics.honeycomb.JvmRootSpanFields;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

import static org.eclipse.microprofile.health.HealthCheckResponse.Status.UP;

@Liveness
@ApplicationScoped
public class ThreadsHealthCheck
                implements HealthCheck
{
    @Override
    public HealthCheckResponse call()
    {
        return new HealthCheckResponse( "threads", UP,
                                        Optional.of( JvmRootSpanFields.getInstance().getThreadsInfo() ) );
    }
}