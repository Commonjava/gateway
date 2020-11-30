package org.commonjava.util.gateway.config;

import org.eclipse.microprofile.config.spi.Converter;

import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class ServiceConfigConverter
                implements Converter<ProxyConfiguration.ServiceConfig>
{
    @Override
    public ProxyConfiguration.ServiceConfig convert( String service )
    {
        if ( isBlank( service ) )
        {
            return null;
        }

        ProxyConfiguration.ServiceConfig ret = new ProxyConfiguration.ServiceConfig();
        for ( String s : service.split( ",|\\n" ) )
        {
            int index = s.indexOf( ":" );
            if ( index < 0 )
            {
                continue;
            }
            String k = s.substring( 0, index ).trim();
            String v = s.substring( index + 1 ).trim();
            switch ( k )
            {
                case "host":
                    ret.host = v;
                    break;
                case "port":
                    ret.port = Integer.parseInt( v );
                    break;
                case "methods":
                    ret.methods = Arrays.asList( v.split( "/" ) );
                    break;
                case "path-pattern":
                    ret.pathPattern = v;
                    break;
                default:
                    break;
            }
        }
        return ret;
    }
}
