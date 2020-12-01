package org.commonjava.util.gateway.config;

import org.eclipse.microprofile.config.spi.Converter;

import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class AbstractConverter<T>
                implements Converter<T>
{
    @Override
    public T convert( String config )
    {
        if ( isBlank( config ) )
        {
            return null;
        }

        T ret = newInstance();
        for ( String s : config.split( ",|\\n" ) )
        {
            int index = s.indexOf( ":" );
            if ( index < 0 )
            {
                continue;
            }
            String k = s.substring( 0, index ).trim();
            String v = s.substring( index + 1 ).trim();
            onEntry( k, v, ret );
        }
        return ret;
    }

    protected abstract T newInstance();

    protected abstract void onEntry( String k, String v, T ret );
}
