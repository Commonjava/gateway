package org.commonjava.util.gateway.cache;

import javax.enterprise.inject.Produces;

public class CacheStrategyProvider
{
    /**
     * Produce cache strategy.
     * TODO: we need a flexible way to plugin custom strategy, e.g, load from Groovy script.
     */
    @Produces
    public CacheStrategy getCacheStrategy()
    {
        return new DefaultCacheStrategy();
    }
}
