package org.commonjava.util.gateway.config;

import io.quarkus.arc.config.ConfigProperties;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ConfigProperties( prefix = "honeycomb" )
public class ProxyHoneycombConfObj
{
    public Optional<Boolean> enabled;

    @ConfigProperty( name = "console-transport" )
    public Optional<Boolean> consoleTransport;

    public Optional<String> dataset;

    @ConfigProperty( name = "write-key" )
    public Optional<String> writeKey;

    @ConfigProperty( name = "base-sample-rate" )
    public Optional<Integer> baseSampleRate;

    public Optional<String> functions;
}
