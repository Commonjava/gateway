package org.commonjava.util.gateway.metrics.honeycomb;

import org.commonjava.o11yphant.honeycomb.RootSpanFields;

import java.util.HashMap;
import java.util.Map;

public class JvmRootSpanFields
                implements RootSpanFields
{
    private final static long MB = 1024 * 1024;

    private final Runtime rt = Runtime.getRuntime();

    @Override
    public Map<String, Object> get()
    {
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = total - free;

        Map<String, Object> ret = new HashMap<>();
        ret.put( "heap_total_mb", total / MB );
        ret.put( "heap_free_mb", free / MB );
        ret.put( "heap_used_mb", used / MB );
        return ret;
    }
}
