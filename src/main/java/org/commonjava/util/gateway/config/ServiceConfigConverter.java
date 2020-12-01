package org.commonjava.util.gateway.config;

import org.eclipse.microprofile.config.spi.Converter;

import java.util.Arrays;

public class ServiceConfigConverter
                extends AbstractConverter<ProxyConfiguration.ServiceConfig>
                implements Converter<ProxyConfiguration.ServiceConfig>
{
    @Override
    protected ProxyConfiguration.ServiceConfig newInstance()
    {
        return new ProxyConfiguration.ServiceConfig();
    }

    @Override
    protected void onEntry( String k, String v, ProxyConfiguration.ServiceConfig ret )
    {
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
}
