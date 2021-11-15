package org.commonjava.util.gateway.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.commonjava.util.gateway.util.WebClientAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.util.gateway.util.ServiceUtils.parseTimeout;

public class ServiceConfig
{
    public String host;

    public int port;

    public boolean ssl;

    public String methods;

    public CacheConfiguration cache;

    @JsonProperty( "path-pattern" )
    public String pathPattern;

    @JsonProperty( "read-timeout-patterns" )
    public String readTimeoutPatterns;

    @JsonIgnore
    private Map<Pattern, Long> timeoutMap = new HashMap<>();

    private Map<String, Duration> cachedTimeouts = new ConcurrentHashMap<>();

    public Map<Pattern, Long> getTimeoutMap()
    {
        return timeoutMap;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
            return true;
        if ( o == null || getClass() != o.getClass() )
            return false;
        ServiceConfig that = (ServiceConfig) o;
        return Objects.equals( methods, that.methods ) && pathPattern.equals( that.pathPattern );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( methods, pathPattern );
    }

    @Override
    public String toString()
    {
        return "ServiceConfig{" + "host='" + host + '\'' + ", port=" + port + ", ssl=" + ssl + ", methods='" + methods
                        + '\'' + ", cache=" + cache + ", pathPattern='" + pathPattern + '\'' + ", readTimeoutPatterns='"
                        + readTimeoutPatterns + '\'' + '}';
    }

    void normalize()
    {
        if ( methods != null )
        {
            methods = methods.toUpperCase();
        }
        if ( cache != null )
        {
            cache.normalize();
        }
        if ( readTimeoutPatterns != null )
        {
            final Logger logger = LoggerFactory.getLogger( getClass() );
            for ( String s : readTimeoutPatterns.split( "," ) )
            {
                if ( isNotBlank( s ) )
                {
                    String[] kv = s.split( "\\|" );
                    String key = kv[0].trim();
                    String val = kv[1].trim();
                    Pattern pattern = Pattern.compile( key );
                    long t = parseTimeout( val );
                    timeoutMap.put( pattern, t );
                    logger.trace( "Add patterned timeout, pattern: {}, timeoutInMillis: {}", key, t );
                }
            }
        }
    }

    // Caching these durations will greatly improve performance, since we don't have to parse / match regular
    // expressions on each request.
    public Duration getMappedTimeout( String path )
    {
        Duration d = cachedTimeouts.get( path );
        if ( d != null )
        {
            return d;
        }
        else
        {
            if ( !timeoutMap.isEmpty() )
            {
                for ( Map.Entry<Pattern, Long> et : timeoutMap.entrySet() )
                {
                    Matcher matcher = et.getKey().matcher( path );
                    if ( matcher.matches() )
                    {
                        Long timeout = et.getValue();
                        d = Duration.ofMillis( timeout );
                        cachedTimeouts.put( path, d );
                    }
                }
            }
        }

        return d;
    }
}
