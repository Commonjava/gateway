package org.commonjava.util.gateway.schedule;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.scheduler.Scheduled;
import org.commonjava.util.gateway.config.ProxyConfiguration;

import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class RefreshProxyConfiguration
{
    @Inject
    private ProxyConfiguration proxyConfiguration;

    @Scheduled( delay = 30, delayUnit = TimeUnit.SECONDS, every = "30s" )
    void refresh()
    {
        proxyConfiguration.load( false );
    }

}
