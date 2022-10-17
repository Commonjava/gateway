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
