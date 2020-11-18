package org.commonjava.util.gateway.config;

import io.quarkus.arc.config.ConfigProperties;

import java.util.List;
import java.util.Objects;

@ConfigProperties( prefix = "proxy" )
public class ProxyServiceConfiguration
{
    public List<ServiceConfig> services;

    public static class ServiceConfig
    {
        public String host;

        public Integer port;

        public List<String> methods;

        public String pathPattern;

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
                return true;
            if ( o == null || getClass() != o.getClass() )
                return false;
            ServiceConfig that = (ServiceConfig) o;
            return host.equals( that.host ) && port.equals( that.port );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( host, port );
        }
    }
}
