package org.commonjava.util.gateway.cache;

import io.smallrye.mutiny.Uni;
import org.apache.commons.io.FileUtils;
import org.commonjava.util.gateway.cache.strategy.DefaultCacheStrategy;
import org.commonjava.util.gateway.cache.strategy.PrefixTrimCacheStrategy;
import org.commonjava.util.gateway.config.ProxyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static java.nio.charset.Charset.defaultCharset;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@ApplicationScoped
public class CacheHandler
{
    private final Logger logger = LoggerFactory.getLogger( CacheHandler.class );

    public Uni<Response> wrapWithCache( Uni<Response> uni, String path, ProxyConfiguration.ServiceConfig service )
    {
        ProxyConfiguration.Cache cache = service.cache;
        if ( cache == null )
        {
            logger.trace( "No cache defined" );
            return uni;
        }

        CacheStrategy cacheStrategy = getCacheStrategy( cache.strategy );
        if ( cacheStrategy.isCache( cache, path ) )
        {
            File cached = cacheStrategy.getCachedFile( cache, path );
            String absolutePath = cached.getAbsolutePath();
            logger.trace( "Search cache, file: {}", absolutePath );
            if ( cached.exists() )
            {
                logger.debug( "Found file in cache, file: {}", absolutePath );
                Uni<Response> ret = null;
                try
                {
                    ret = renderCachedFile( cache, cached );
                }
                catch ( IOException e )
                {
                    logger.warn( "Failed to render cached file", e );
                }
                if ( ret != null )
                {
                    return ret;
                }
            }
        }

        if ( cacheStrategy.isCacheForWrite( cache, path ) )
        {
            uni = uni.onItem().invoke( resp -> {
                if ( resp.getStatus() == OK.getStatusCode() )
                {
                    writeToCache( resp, cache, path );
                }
            } );
        }
        return uni;
    }

    private CacheStrategy getCacheStrategy( String strategy )
    {
        if ( isNotBlank( strategy ) )
        {
            if ( PrefixTrimCacheStrategy.class.getName().contains( strategy ) )
            {
                return PrefixTrimCacheStrategy.INSTANCE;
            }
        }
        return DefaultCacheStrategy.INSTANCE;
    }

    private Uni<Response> renderCachedFile( ProxyConfiguration.Cache cache, File cached ) throws IOException
    {
        long expire = cache.getExpireInSeconds();
        if ( expire > 0 )
        {
            Path file = cached.toPath();
            BasicFileAttributes attr = Files.readAttributes( file, BasicFileAttributes.class );
            long seconds = attr.lastModifiedTime().to( TimeUnit.SECONDS );
            if ( currentTimeMillis() / 1000 - seconds > expire ) // expired
            {
                logger.debug( "File expired, file: {}", file );
                FileUtils.deleteQuietly( cached );
                FileUtils.deleteQuietly( getMetadataFile( cached ) );
                return null;
            }
            else
            {
                return renderFile( cached );
            }
        }
        return renderFile( cached );
    }

    private Uni<Response> renderFile( File cached ) throws IOException
    {
        logger.debug( "Render file, {}", cached.getPath() );
        Response.ResponseBuilder resp = Response.ok( cached );
        File httpMetadata = getMetadataFile( cached );
        if ( httpMetadata.exists() )
        {
            List<String> lines = FileUtils.readLines( httpMetadata, DEFAULT_CHARSET );
            lines.forEach( line -> {
                if ( isNotBlank( line ) )
                {
                    String[] header = line.split( "=" );
                    if ( header.length >= 2 )
                    {
                        resp.header( header[0], header[1] );
                    }
                }
            } );
        }
        return Uni.createFrom().item( resp.build() );
    }

    private static final Charset DEFAULT_CHARSET = defaultCharset();

    private void writeToCache( Response resp, ProxyConfiguration.Cache cache, String path )
    {
        CacheStrategy cacheStrategy = getCacheStrategy( cache.strategy );

        File f = cacheStrategy.getCachedFile( cache, path );
        File metadata = getMetadataFile( f );

        logger.debug( "Write to file: {}", f );
        Object entity = resp.getEntity();
        if ( entity instanceof byte[] )
        {
            byte[] bytes = (byte[]) entity;
            try
            {
                FileUtils.writeByteArrayToFile( f, bytes );
                FileUtils.writeStringToFile( metadata, getHeadersAsString( resp.getStringHeaders() ), DEFAULT_CHARSET );
            }
            catch ( IOException e )
            {
                logger.warn( "Can not write cache file", e );
            }
        }
    }

    private static final String _HTTP_METADATA = ".http-metadata";

    private static File getMetadataFile( File f )
    {
        return new File( f.getAbsolutePath() + _HTTP_METADATA );
    }

    private static String getHeadersAsString( MultivaluedMap<String, String> headers )
    {
        StringBuilder sb = new StringBuilder();
        headers.entrySet().forEach( et -> {
            // remove '[]' from value string like "Content-Encoding=[gzip]"
            String v = et.getValue().toString().replaceAll( "(\\[|\\])", "" );
            sb.append( et.getKey() + "=" + v + "\n" );
        } );
        return sb.toString();
    }


}