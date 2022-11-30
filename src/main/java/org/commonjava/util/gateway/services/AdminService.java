/**
 * Copyright (C) 2022 John Casey (jdcasey@commonjava.org)
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
package org.commonjava.util.gateway.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import org.commonjava.util.gateway.config.ProxyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.join;

@ApplicationScoped
public class AdminService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    ProxyConfiguration serviceConfiguration;

    private static final String VERSIONING_PROPERTIES = "version.properties";

    final Properties info = new Properties();

    @PostConstruct
    void init()
    {
        ClassLoader cl = this.getClass().getClassLoader();

        try (InputStream is = cl.getResourceAsStream( VERSIONING_PROPERTIES ))
        {
            if ( is != null )
            {
                info.load( is );
            }
            else
            {
                logger.warn( "Resource not found, file: {}, loader: {}", VERSIONING_PROPERTIES, cl );
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to read version information from classpath resource: " + VERSIONING_PROPERTIES, e );
        }
    }

    public Uni<JsonObject> getProxyConfig()
    {
        return Uni.createFrom().item( JsonObject.mapFrom( serviceConfiguration ) );
    }

    public Uni<JsonObject> getProxyInfo()
    {
        return Uni.createFrom().item( JsonObject.mapFrom( info ) );
    }

    public Uni<String> getThreadDumpString()
    {
        return Uni.createFrom().item( getThreadDumpString( null ) ); // all states
    }

    public String getThreadDumpString( Thread.State state )
    {
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate( threads );

        Map<Long, Thread> threadMap = new HashMap<>();
        Stream.of( threads ).forEach( t -> threadMap.put( t.getId(), t ) );
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo( threadMXBean.getAllThreadIds(), Integer.MAX_VALUE );

        StringBuilder sb = new StringBuilder();
        Stream.of( threadInfos ).forEachOrdered( ( ti ) -> {
            if ( state == null || state == ti.getThreadState() )
            {
                appendThreadInfo( sb, threadMap, ti );
            }
        } );

        return sb.toString();
    }

    private void appendThreadInfo( StringBuilder sb, Map<Long, Thread> threadMap, ThreadInfo ti )
    {
        if ( sb.length() > 0 )
        {
            sb.append( "\n\n" );
        }

        String threadGroup = "Unknown";
        Thread t = threadMap.get( ti.getThreadId() );
        if ( t != null )
        {
            ThreadGroup tg = t.getThreadGroup();
            if ( tg != null )
            {
                threadGroup = tg.getName();
            }
        }

        sb.append( ti.getThreadName() )
          .append( "\n  Group: " )
          .append( threadGroup )
          .append( "\n  State: " )
          .append( ti.getThreadState() )
          .append( "\n  Lock Info: " )
          .append( ti.getLockInfo() )
          .append( "\n  Monitors:" );

        MonitorInfo[] monitors = ti.getLockedMonitors();
        if ( monitors == null || monitors.length < 1 )
        {
            sb.append( "  -NONE-" );
        }
        else
        {
            sb.append( "\n  - " ).append( join( monitors, "\n  - " ) );
        }

        sb.append( "\n  Trace:\n    " ).append( join( ti.getStackTrace(), "\n    " ) );
    }

}
