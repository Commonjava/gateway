package org.commonjava.util.gateway.config;

import io.quarkus.arc.config.ConfigProperties;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ConfigProperties( prefix = "proxy" )
public class ProxyConfiguration
{
    public Optional<Retry> retry;

    public List<ServiceConfig> services;

    @Override
    public String toString()
    {
        return "ProxyConfiguration{" + "retry=" + retry + ", services=" + services + '}';
    }

    public static class ServiceConfig
    {
        public String host;

        public int port;

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
            return port == that.port && host.equals( that.host );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( host, port );
        }

        @Override
        public String toString()
        {
            return "ServiceConfig{" + "host='" + host + '\'' + ", port=" + port + ", methods=" + methods
                            + ", pathPattern='" + pathPattern + '\'' + '}';
        }
    }

    public static class Retry
    {
        public int count;

        public long interval;

        @Override
        public String toString()
        {
            return "Retry{" + "count=" + count + ", interval=" + interval + '}';
        }
    }
}
