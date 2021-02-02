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
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.util.gateway.services.ProxyConstants.EVENT_PROXY_CONFIG_CHANGE;

@Startup
@ApplicationScoped
public class ProxyConfiguration
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String USER_DIR = System.getProperty( "user.dir" ); // where the JVM was invoked

    @Inject
    transient EventBus bus;

    @JsonProperty( "read-timeout" )
    private String readTimeout;

    public String getReadTimeout()
    {
        return readTimeout;
    }

    private volatile Retry retry;

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
     * Load proxy config from '${user.dir}/config/proxy.yaml'. If not found, load from default classpath resource.
     */
    public void load( boolean init )
    {
        File file = new File( USER_DIR, "config/" + PROXY_YAML );
        if ( file.exists() )
        {
            logger.info( "Load proxy config from file, {}", file );
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
        else if ( init )
        {
            logger.info( "Load proxy config from classpath resource, {}", PROXY_YAML );
            InputStream res = this.getClass().getClassLoader().getResourceAsStream( PROXY_YAML );
            if ( res != null )
            {
                doLoad( res );
            }
        }
        else
        {
            logger.info( "Skip loading proxy config - no such file: {}", file );
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

            this.retry = parsed.retry;

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

        public boolean ssl;

        public String methods;

        public Cache cache;

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
            return "ServiceConfig{" + "host='" + host + '\'' + ", port=" + port + ", ssl=" + ssl + ", methods='"
                            + methods + '\'' + ", cache=" + cache + ", pathPattern='" + pathPattern + '\'' + '}';
        }

        private void normalize()
        {
            if ( methods != null )
            {
                methods = methods.toUpperCase();
            }
            if ( cache != null )
            {
                cache.normalize();
            }
        }
    }

    public static class Retry
    {
        public int count;

        public long interval; // in millis

        @Override
        public String toString()
        {
            return "Retry{" + "count=" + count + ", interval=" + interval + '}';
        }

    }

    public static class Cache
    {
        public boolean enabled;

        // true if only read from pre-seed cache, or write to cache for each successful GET request
        public boolean readonly;

        // only files match the pattern are cached, null for all files
        public String pattern;

        // expiration in PnDTnHnMn, as parsed by java.time.Duration
        public String expire;

        // cache dir, default ${runtime_root}/cache
        public String dir;

        private void normalize()
        {
            if ( isNotBlank( expire ) )
            {
                String ls = expire.toLowerCase();
                String prefix;
                if ( ls.contains( "d" ) )
                {
                    prefix = "P";
                }
                else
                {
                    prefix = "PT";
                }
                expireInSeconds = Duration.parse( prefix + expire ).getSeconds();
            }
        }

        @Override
        public String toString()
        {
            return "Cache{" + "enabled=" + enabled + ", readonly=" + readonly + ", pattern='" + pattern + '\''
                            + ", expire='" + expire + '\'' + ", dir='" + dir + '\'' + ", expireInSeconds="
                            + expireInSeconds + '}';
        }

        private transient long expireInSeconds;

        public long getExpireInSeconds()
        {
            return expireInSeconds;
        }
    }
}
