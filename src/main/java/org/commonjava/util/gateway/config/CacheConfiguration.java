package org.commonjava.util.gateway.config;

import java.time.Duration;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class CacheConfiguration
{
    public boolean enabled;

    // True if only read from pre-seed cache, or write to cache for each successful GET request
    public boolean readonly;

    // Only files match the pattern are cached, null for all files
    public String pattern;

    // Expiration in PnDTnHnMn, as parsed by java.time.Duration
    public String expire;

    // Cache dir, default ${runtime_root}/cache
    public String dir;

    // Cache strategy class name, e.g, PrefixTrimCacheStrategy (or simply PrefixTrim). If not set, use default.
    public String strategy;

    void normalize()
    {
        if ( isNotBlank( expire ) )
        {
            String ls = expire.toLowerCase();
            String prefix;
            if ( ls.contains( "d" ) )
            {
                prefix = "P";
            }
            else
            {
                prefix = "PT";
            }
            expireInSeconds = Duration.parse( prefix + expire ).getSeconds();
        }
        if ( isNotBlank( pattern ) )
        {
            compiledPattern = Pattern.compile( pattern );
        }
    }

    @Override
    public String toString()
    {
        return "Cache{" + "enabled=" + enabled + ", readonly=" + readonly + ", pattern='" + pattern + '\''
                        + ", expire='" + expire + '\'' + ", dir='" + dir + '\'' + ", strategy='" + strategy + '\''
                        + ", expireInSeconds=" + expireInSeconds + '}';
    }

    private transient long expireInSeconds;

    public long getExpireInSeconds()
    {
        return expireInSeconds;
    }

    private transient Pattern compiledPattern;

    public Pattern getCompiledPattern()
    {
        return compiledPattern;
    }
}
