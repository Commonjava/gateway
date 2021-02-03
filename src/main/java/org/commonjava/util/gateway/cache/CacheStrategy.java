package org.commonjava.util.gateway.cache;

import org.commonjava.util.gateway.config.ProxyConfiguration;

import java.io.File;

public interface CacheStrategy
{
    /**
     * Whether the specific path should be read from cache.
     */
    boolean isCache( ProxyConfiguration.Cache cache, String path );

    /**
     * Whether the specific path should be written to cache after a successful download.
     */
    boolean isCacheForWrite( ProxyConfiguration.Cache cache, String path );

    /**
     * Get the file to read/write from/to the cache.
     */
    File getCachedFile( ProxyConfiguration.Cache cache, String path );
}
