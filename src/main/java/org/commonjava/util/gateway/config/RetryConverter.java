package org.commonjava.util.gateway.config;

import org.eclipse.microprofile.config.spi.Converter;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class RetryConverter
                implements Converter<ProxyConfiguration.Retry>
{

    @Override
    public ProxyConfiguration.Retry convert( String config )
    {
        if ( isBlank( config ) )
        {
            return null;
        }

        ProxyConfiguration.Retry ret = new ProxyConfiguration.Retry();
        for ( String s : config.split( ",|\\n" ) )
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
        return ret;
    }
}
