package org.commonjava.util.gateway.util;

import org.commonjava.util.gateway.config.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceUtils
{
    private final static Logger logger = LoggerFactory.getLogger( ServiceUtils.class );

    public static long parseTimeout( String timeout )
    {
        return Duration.parse( "pt" + timeout ).toMillis();
    }
}
