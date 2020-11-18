package org.commonjava.util.gateway;

import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.util.gateway.config.ProxyServiceConfiguration;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@QuarkusTest
public class ConfigurationTest
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ProxyServiceConfiguration serviceConfiguration;

    @Test
    public void test()
    {
        serviceConfiguration.services.forEach( service -> {
            logger.debug("Service: {}, {}, {}, {}", service.host, service.port, service.methods, service.pathPattern);
        } );
    }

}