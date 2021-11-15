package org.commonjava.util.gateway.cache;

import org.commonjava.util.gateway.config.CacheConfiguration;

import java.io.File;

public interface CacheStrategy
{
    /**
     * Whether the specific path should be read from cache.
     */
    boolean isCache( CacheConfiguration cache, String path );

    /**
     * Whether the specific path should be written to cache after a successful download.
     */
    boolean isCacheForWrite( CacheConfiguration cache, String path );

    /**
     * Get the file to read/write from/to the cache.
     */
    File getCachedFile( CacheConfiguration cache, String path );
}
