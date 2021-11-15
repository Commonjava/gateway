package org.commonjava.util.gateway.metrics.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;

import static org.eclipse.microprofile.health.HealthCheckResponse.Status.UP;

@Liveness
@ApplicationScoped
public class HeapHealthCheck
                implements HealthCheck
{
    @Inject
    JvmHealthInfo jvmInfo;

    @Override
    public HealthCheckResponse call()
    {
        return new HealthCheckResponse( "heap", UP, Optional.of( jvmInfo.getHeapInfo() ) );
    }
}