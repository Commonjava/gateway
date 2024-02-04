/**
 * Copyright (C) 2022 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
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
package org.commonjava.util.gateway.cache.strategy;

import org.commonjava.util.gateway.cache.CacheStrategy;
import org.commonjava.util.gateway.config.CacheConfiguration;
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
    public File getCachedFile( CacheConfiguration cache, String path )
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
