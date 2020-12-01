package org.commonjava.util.gateway.config;

import org.eclipse.microprofile.config.spi.Converter;

public class RetryConverter
                extends AbstractConverter<ProxyConfiguration.Retry>
                implements Converter<ProxyConfiguration.Retry>
{
    @Override
    protected ProxyConfiguration.Retry newInstance()
    {
        return new ProxyConfiguration.Retry();
    }

    @Override
    protected void onEntry( String k, String v, ProxyConfiguration.Retry ret )
    {
        switch ( k )
        {
            case "count":
                ret.count = Integer.parseInt( v );
                break;
            case "interval":
                ret.interval = Long.parseLong( v );
                break;
            default:
                break;
        }
    }
}
