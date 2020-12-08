package org.commonjava.util.gateway.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import org.commonjava.util.gateway.config.ProxyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@ApplicationScoped
public class AdminService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    ProxyConfiguration serviceConfiguration;

    private static final String VERSIONING_PROPERTIES = "version.properties";

    final Properties info = new Properties();

    @PostConstruct
    void init()
    {
        ClassLoader cl = this.getClass().getClassLoader();

        try (InputStream is = cl.getResourceAsStream( VERSIONING_PROPERTIES ))
        {
            if ( is != null )
            {
                info.load( is );
            }
            else
            {
                logger.warn( "Resource not found, file: {}, loader: {}", VERSIONING_PROPERTIES, cl );
            }
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to read version information from classpath resource: " + VERSIONING_PROPERTIES, e );
        }
    }

    public Uni<JsonObject> getProxyConfig()
    {
        return Uni.createFrom().item( JsonObject.mapFrom( serviceConfiguration ) );
    }

    public Uni<JsonObject> getProxyInfo()
    {
        return Uni.createFrom().item( JsonObject.mapFrom( info ) );
    }
}
