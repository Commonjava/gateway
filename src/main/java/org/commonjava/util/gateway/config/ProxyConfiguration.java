package org.commonjava.util.gateway.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.Startup;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.commonjava.util.gateway.services.ProxyConstants.EVENT_PROXY_CONFIG_CHANGE;

@Startup
@ApplicationScoped
public class ProxyConfiguration
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    transient EventBus bus;

    @JsonProperty( "read-timeout" )
    private String readTimeout;

    public String getReadTimeout()
    {
        return readTimeout;
    }

    private Retry retry;

    private Set<ServiceConfig> services = Collections.synchronizedSet( new HashSet<>() );

    public Set<ServiceConfig> getServices()
    {
        return services;
    }

    public Retry getRetry()
    {
        return retry;
    }

    @Override
    public String toString()
    {
        return "ProxyConfiguration{" + "readTimeout='" + readTimeout + '\'' + ", retry=" + retry + ", services="
                        + services + '}';
    }

    @PostConstruct
    void init()
    {
        load( true );
        logger.info( "Proxy config, {}", this );
    }

    private static final String PROXY_YAML = "proxy.yaml";

    /**
     * Load proxy config from classpath resource (if init is true) and '${user.dir}/config/proxy.yaml'.
     */
    public void load( boolean init )
    {
        if ( init )
        {
            InputStream res = this.getClass().getClassLoader().getResourceAsStream( PROXY_YAML );
            if ( res != null )
            {
                logger.info( "Load from classpath, {}", PROXY_YAML );
                doLoad( res );
            }
        }

        loadFromFile();
    }

    private void loadFromFile()
    {
        String userDir = System.getProperty( "user.dir" ); // where the JVM was invoked
        File file = new File( userDir, "config/" + PROXY_YAML );
        if ( file.exists() )
        {
            logger.info( "Load from file, {}", file );
            try
            {
                doLoad( new FileInputStream( file ) );
            }
            catch ( FileNotFoundException e )
            {
                logger.error( "Load failed", e );
                return;
            }
        }
        else
        {
            logger.info( "Skip load, NO_SUCH_FILE, {}", file );
        }
    }

    private transient String md5Hex; // used to check whether the custom proxy.yaml has changed

    private void doLoad( InputStream res )
    {
        try
        {
            String str = IOUtils.toString( res, UTF_8 );
            String md5 = DigestUtils.md5Hex( str ).toUpperCase();
            if ( md5.equals( md5Hex ) )
            {
                logger.info( "Skip, NO_CHANGE" );
                return;
            }

            ProxyConfiguration parsed = parseConfig( str );
            logger.info( "Loaded: {}", parsed );

            if ( parsed.readTimeout != null )
            {
                this.readTimeout = parsed.readTimeout;
            }

            if ( this.retry == null )
            {
                this.retry = parsed.retry;
            }
            else if ( parsed.retry != null )
            {
                this.retry.copyFrom( parsed.retry );
            }

            if ( parsed.services != null )
            {
                parsed.services.forEach( sv -> {
                    overrideIfPresent( sv );
                } );
            }

            if ( md5Hex != null )
            {
                bus.publish( EVENT_PROXY_CONFIG_CHANGE, "" );
            }

            md5Hex = md5;
        }
        catch ( IOException e )
        {
            logger.error( "Load failed", e );
        }
    }

    private void overrideIfPresent( ServiceConfig sv )
    {
        this.services.remove( sv ); // remove first so it can replace the old one
        this.services.add( sv );
    }

    private ProxyConfiguration parseConfig( String str )
    {
        Yaml yaml = new Yaml();
        Map<String, Object> obj = yaml.load( str );
        Map<String, Object> proxy = (Map) obj.get( "proxy" );
        JsonObject jsonObject = JsonObject.mapFrom( proxy );
        ProxyConfiguration ret = jsonObject.mapTo( this.getClass() );
        if ( ret.services != null )
        {
            ret.services.forEach( sv -> sv.normalize() );
        }
        return ret;
    }

    public static class ServiceConfig
    {
        public String host;

        public int port;

        public String methods;

        @JsonProperty( "path-pattern" )
        public String pathPattern;

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
                return true;
            if ( o == null || getClass() != o.getClass() )
                return false;
            ServiceConfig that = (ServiceConfig) o;
            return Objects.equals( methods, that.methods ) && pathPattern.equals( that.pathPattern );
        }

        @Override
        public int hashCode()
        {
            return Objects.hash( methods, pathPattern );
        }

        @Override
        public String toString()
        {
            return "ServiceConfig{" + "host='" + host + '\'' + ", port=" + port + ", methods='" + methods + '\''
                            + ", pathPattern='" + pathPattern + '\'' + '}';
        }

        void normalize()
        {
            if ( methods != null )
            {
                methods = methods.toUpperCase();
            }
        }
    }

    public static class Retry
    {
        public volatile int count;

        public volatile long interval; // in millis

        @Override
        public String toString()
        {
            return "Retry{" + "count=" + count + ", interval=" + interval + '}';
        }

        public void copyFrom( Retry retry )
        {
            count = retry.count;
            interval = retry.interval;
        }
    }
}
