/**
 * Copyright (C) 2020 John Casey (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
