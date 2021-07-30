package org.commonjava.util.gateway.util;

import org.commonjava.util.gateway.config.ProxyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceUtils
{
    private final static Logger logger = LoggerFactory.getLogger( ServiceUtils.class );

    public static long parseTimeout( String timeout )
    {
        return Duration.parse( "pt" + timeout ).toMillis();
    }

    /**
     * Get timeout according to path patterns, e.g., .+/promote -> 30m, ...
     */
    public static long getTimeout( ProxyConfiguration.ServiceConfig serviceConfig, String path, long defaultTimeout )
    {
        if ( !serviceConfig.getTimeoutMap().isEmpty() )
        {
            //logger.trace( "Check timeout, map:{}", serviceConfig.getTimeoutMap() );
            for ( Map.Entry<Pattern, Long> et : serviceConfig.getTimeoutMap().entrySet() )
            {
                Matcher matcher = et.getKey().matcher( path );
                if ( matcher.matches() )
                {
                    logger.trace( "Get patterned timeout, path:{}, timeout:{}", path, et.getValue() );
                    return et.getValue();
                }
            }
        }
        logger.trace( "Return default timeout, path:{}, timeout:{}", path, defaultTimeout );
        return defaultTimeout;
    }
}
