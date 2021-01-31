package org.commonjava.util.gateway.util;

import io.smallrye.mutiny.Uni;
import org.apache.commons.io.FileUtils;
import org.commonjava.util.gateway.config.ProxyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static org.commonjava.util.gateway.config.ProxyConfiguration.USER_DIR;

public class CacheUtils
{
    private static final Logger logger = LoggerFactory.getLogger( CacheUtils.class );

    public static final File DEFAULT_CACHE_DIR = new File( USER_DIR, "cache" );

    public static Uni<Response> wrapWithCache( Uni<Response> uni, String path,
                                               ProxyConfiguration.ServiceConfig service )
    {
        if ( isCache( service.cache, path ) )
        {
            File cached = getCachedFile( service.cache, path );
            String absolutePath = cached.getAbsolutePath();
            logger.trace( "Search cache, file: {}", absolutePath );
            if ( cached.exists() )
            {
                logger.debug( "Found file in cache, file: {}", absolutePath );
                Uni<Response> ret = null;
                try
                {
                    ret = renderCachedFile( service.cache, cached );
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

        if ( isCacheForWrite( service.cache, path ) )
        {
            uni = uni.onItem().invoke( resp -> {
                if ( resp.getStatus() == OK.getStatusCode() )
                {
                    writeToCache( resp, service.cache, path );
                }
            } );
        }
        return uni;
    }

    private static Uni<Response> renderCachedFile( ProxyConfiguration.Cache cache, File cached ) throws IOException
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

    private static Uni<Response> renderFile( File cached ) throws IOException
    {
        Response.ResponseBuilder resp = Response.ok( cached );
        List<String> lines = FileUtils.readLines( getMetadataFile( cached ), DEFAULT_CHARSET );
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
        return Uni.createFrom().item( resp.build() );
    }

    private static final Charset DEFAULT_CHARSET = defaultCharset();

    private static void writeToCache( Response resp, ProxyConfiguration.Cache cache, String path )
    {
        File f = getCachedFile( cache, path );
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

    private static File getCachedFile( ProxyConfiguration.Cache cache, String path )
    {
        File f;
        if ( isNotBlank( cache.dir ) )
        {
            f = new File( cache.dir, path );
        }
        else
        {
            f = new File( DEFAULT_CACHE_DIR, path );
        }
        return f;
    }

    private static boolean isCache( ProxyConfiguration.Cache cache, String path )
    {
        if ( cache != null && cache.enabled && path.matches( cache.pattern ) )
        {
            logger.trace( "Cache matches (read): {}", path );
            return true;
        }
        return false;
    }

    private static boolean isCacheForWrite( ProxyConfiguration.Cache cache, String path )
    {
        if ( cache != null && cache.enabled && !cache.readonly && path.matches( cache.pattern ) )
        {
            logger.trace( "Cache matches (write): {}", path );
            return true;

        }
        return false;
    }

}