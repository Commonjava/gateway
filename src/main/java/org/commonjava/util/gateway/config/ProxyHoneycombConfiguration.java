package org.commonjava.util.gateway.config;

import io.quarkus.runtime.Startup;
import org.commonjava.o11yphant.honeycomb.config.HoneycombConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Collections.EMPTY_MAP;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.CLIENT_ADDR;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.HTTP_METHOD;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.HTTP_STATUS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REQUEST_LATENCY_MILLIS;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.REST_ENDPOINT_PATH;
import static org.commonjava.o11yphant.metrics.RequestContextConstants.TRACE_ID;

@ApplicationScoped
@Startup
public class ProxyHoneycombConfiguration
                implements HoneycombConfiguration
{
    public static final String ERROR_MESSAGE = "error_message";

    public static final String ERROR_CLASS = "error_class";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Set<String> fieldSet;

    @Override
    public Set<String> getFieldSet()
    {
        return fieldSet;
    }

    @Inject
    ProxyHoneycombConfObj confObj;

    private Map<String, Function> functionMap = EMPTY_MAP;

    private Map<String, Integer> sampleRates = EMPTY_MAP; // made from functionMap for convenience

    @PostConstruct
    void init()
    {
        String functions = confObj.functions.orElse( "" );
        if ( isNotBlank( functions ) )
        {
            logger.info( "Set honeycomb configuration, functions: {}", functions );
            Map<String, Function> m = new HashMap<>();
            String[] toks = functions.split( "," );
            for ( String s : toks )
            {
                Function f = Function.parse( s );
                m.put( f.name, f );
            }
            functionMap = Collections.unmodifiableMap( m );

            sampleRates = new HashMap<>();
            functionMap.entrySet().forEach( et -> sampleRates.put( et.getKey(), et.getValue().sampleRate ) );
        }

        fieldSet = Collections.unmodifiableSet(
                        new HashSet<>( Arrays.asList( HTTP_METHOD, HTTP_STATUS, TRACE_ID, CLIENT_ADDR,
                                                      REST_ENDPOINT_PATH, REQUEST_LATENCY_MILLIS, ERROR_CLASS,
                                                      ERROR_MESSAGE ) ) );
    }

    @Override
    public String getServiceName()
    {
        return "gateway";
    }

    @Override
    public boolean isEnabled()
    {
        return confObj.enabled.orElse( false );
    }

    @Override
    public String getWriteKey()
    {
        return confObj.writeKey.orElse( null );
    }

    @Override
    public String getDataset()
    {
        return confObj.dataset.orElse( null );
    }

    @Override
    public Integer getBaseSampleRate()
    {
        return confObj.baseSampleRate.orElse( 0 );
    }

    @Override
    public String getNodeId()
    {
        return null;
    }

    @Override
    public boolean isConsoleTransport()
    {
        return confObj.consoleTransport.orElse( false );
    }

    private static class Function
    {
        Pattern pattern;

        String name;

        Integer sampleRate;

        public static Function parse( String s )
        {
            String[] toks = s.split( "\\|" );

            Function ret = new Function();
            ret.pattern = Pattern.compile( toks[0].trim() );
            ret.name = toks[1].trim();
            if ( toks.length > 2 )
            {
                ret.sampleRate = Integer.parseInt( toks[2].trim() );
            }
            return ret;
        }

        @Override
        public String toString()
        {
            return "Function{" + "pattern=" + pattern + ", name='" + name + '\'' + ", sampleRate=" + sampleRate + '}';
        }
    }

    public String getFunctionName( String path )
    {
        for ( Function f : functionMap.values() )
        {
            if ( f.pattern.matcher( path ).matches() )
            {
                return f.name;
            }
        }
        return "other";
    }

    @Override
    public Map<String, Integer> getSpanRates()
    {
        return sampleRates;
    }

}
