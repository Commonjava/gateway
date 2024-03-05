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
package org.commonjava.util.gateway.metrics.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class JvmHealthInfo
{
    private final static long MB = 1024 * 1024;

    private final Runtime rt = Runtime.getRuntime();

    private final ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();

    private JvmHealthInfo()
    {
    }

    public Map<String, Object> getHeapInfo()
    {
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;

        Map<String, Object> ret = new HashMap<>();
        ret.put( "total_mb", total / MB );
        ret.put( "free_mb", free / MB );
        ret.put( "used_mb", used / MB );
        return ret;
    }

    public Map<String, Object> getThreadsInfo()
    {
        Map<String, Object> ret = new HashMap<>();
        ThreadInfo[] threads = mxBean.getThreadInfo( mxBean.getAllThreadIds() );

        for ( ThreadInfo info : threads )
        {
            String state = info.getThreadState().toString().toLowerCase();
            Long count = (Long) ret.get( state );
            if ( count == null )
            {
                count = 1L;
            }
            else
            {
                count += 1;
            }
            ret.put( state, count );
        }
        return ret;
    }
}
