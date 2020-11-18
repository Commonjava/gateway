package org.commonjava.util.gateway.config;

import org.eclipse.microprofile.config.spi.Converter;

import java.util.Arrays;

public class ServiceConfigConverter
                implements Converter<ProxyServiceConfiguration.ServiceConfig>
{
    @Override
    public ProxyServiceConfiguration.ServiceConfig convert( String service )
    {
        ProxyServiceConfiguration.ServiceConfig ret = new ProxyServiceConfiguration.ServiceConfig();
        for ( String s : service.split( "," ) )
        {
            int index = s.indexOf( ":" );
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
