package org.commonjava.util.gateway.cache;

import org.commonjava.util.gateway.config.ProxyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Alternative;
import java.io.File;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.util.gateway.config.ProxyConfiguration.USER_DIR;

@Alternative
public class DefaultCacheStrategy implements CacheStrategy
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final File DEFAULT_CACHE_DIR = new File( USER_DIR, "cache" );

    @Override
    public boolean isCache( ProxyConfiguration.Cache cache, String path )
    {
        if ( cache != null && cache.enabled && path.matches( cache.pattern ) )
        {
            logger.trace( "Cache matches (read): {}", path );
            return true;
        }
        return false;
    }

    @Override
    public boolean isCacheForWrite( ProxyConfiguration.Cache cache, String path )
    {
        if ( cache != null && cache.enabled && !cache.readonly && path.matches( cache.pattern ) )
        {
            logger.trace( "Cache matches (write): {}", path );
            return true;

        }
        return false;
    }

    @Override
    public File getCachedFile( ProxyConfiguration.Cache cache, String path )
    {
        File f;
        if ( isNotBlank( cache.dir ) )
        {
            f = new File( cache.dir, path );
        }
        else
        {
            f = new File( DEFAULT_CACHE_DIR, path );
        }
        return f;
    }

}
