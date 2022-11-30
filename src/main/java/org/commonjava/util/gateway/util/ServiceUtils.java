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
package org.commonjava.util.gateway.util;

import io.vertx.core.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.time.Duration;

import static java.nio.charset.Charset.defaultCharset;

public class ServiceUtils
{
    private final static Logger logger = LoggerFactory.getLogger( ServiceUtils.class );

    public static long parseTimeout( String timeout )
    {
        return Duration.parse( "pt" + timeout ).toMillis();
    }

    public static String pathWithParams( String path, final String params )
    {
        final StringBuilder sb = new StringBuilder( path );
        if ( params != null && !params.isEmpty() )
        {
            sb.append("?" + params);
        }
        return sb.toString();
    }
}
