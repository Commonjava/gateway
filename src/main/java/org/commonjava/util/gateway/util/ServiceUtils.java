package org.commonjava.util.gateway.util;

import io.vertx.core.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class ServiceUtils
{
    private final static Logger logger = LoggerFactory.getLogger( ServiceUtils.class );

    public static long parseTimeout( String timeout )
    {
        return Duration.parse( "pt" + timeout ).toMillis();
    }

    public static String pathWithParams( String path, final MultiMap params )
    {
        final StringBuilder sb = new StringBuilder( path );
        if ( params != null && !params.isEmpty() )
        {
            sb.append("?");
            params.entries().forEach(entry -> {
                sb.append( entry.getKey() ).append("=").append(entry.getValue()).append("&");
            } );
            sb.deleteCharAt(sb.length() - 1); // delete last '&'
        }
        return sb.toString();
    }
}
