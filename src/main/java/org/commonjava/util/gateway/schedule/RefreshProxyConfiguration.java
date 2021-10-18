package org.commonjava.util.gateway.schedule;

import io.quarkus.scheduler.Scheduled;
import org.commonjava.util.gateway.config.ProxyConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class RefreshProxyConfiguration
{
    @Inject
    ProxyConfiguration proxyConfiguration;

    @Scheduled( delay = 60, delayUnit = TimeUnit.SECONDS, every = "60s" )
    void refresh()
    {
        proxyConfiguration.load( false );
    }

}
