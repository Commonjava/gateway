package org.commonjava.util.gateway.cache.strategy;

import org.commonjava.util.gateway.cache.CacheStrategy;
import org.commonjava.util.gateway.config.ProxyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Alternative;
import java.io.File;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Alternative
public class PrefixTrimCacheStrategy
                extends DefaultCacheStrategy
{
    public static final CacheStrategy INSTANCE = new PrefixTrimCacheStrategy();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static String pathPrefix = System.getProperty( "pathPrefixToTrim" ); // set vis -DpathPrefixToTrim

    @Override
    public File getCachedFile( ProxyConfiguration.Cache cache, String path )
    {
        if ( isNotBlank( pathPrefix ) )
        {
            int index = path.lastIndexOf( pathPrefix );
            if ( index >= 0 )
            {
                path = path.substring( index + pathPrefix.length() );
                logger.debug( "Trim path prefix, trimmed: {}", path );
            }
        }
        return super.getCachedFile( cache, path );
    }

}
