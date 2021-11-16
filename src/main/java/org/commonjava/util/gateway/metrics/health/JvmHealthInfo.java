package org.commonjava.util.gateway.metrics.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
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
