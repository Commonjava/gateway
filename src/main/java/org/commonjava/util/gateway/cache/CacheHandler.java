/**
 * Copyright (C) 2022 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.util.gateway.cache;

import io.opentelemetry.api.trace.Span;
import io.smallrye.mutiny.Uni;
import org.apache.commons.io.FileUtils;
import org.commonjava.util.gateway.cache.strategy.DefaultCacheStrategy;
import org.commonjava.util.gateway.cache.strategy.PrefixTrimCacheStrategy;
import org.commonjava.util.gateway.config.CacheConfiguration;
import org.commonjava.util.gateway.config.ServiceConfig;
import org.commonjava.util.gateway.util.OtelAdapter;
import org.commonjava.util.gateway.util.ProxyStreamingOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
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

    @Inject
    OtelAdapter otel;

    public Uni<Response> wrapWithCache( Uni<Response> uni, String path, ServiceConfig service )
    {
        CacheConfiguration cache = service.cache;
        if ( cache == null )
        {
            Span.current().setAttribute( "cached", 0 );

            logger.trace( "No cache defined" );
            return uni;
        }

        Span.current().setAttribute( "cache_strategy", cache.strategy );

        CacheStrategy cacheStrategy = getCacheStrategy( cache.strategy );
        if ( cacheStrategy.isCache( cache, path ) )
        {
            Span.current().setAttribute( "cached", 1 );

            File cached = cacheStrategy.getCachedFile( cache, path );
            String absolutePath = cached.getAbsolutePath();
            logger.trace( "Search cache, file: {}", absolutePath );
            if ( cached.exists() )
            {
                Span.current().setAttribute( "served_from", "cache" );

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
            Span.current().setAttribute( "cached", 1 );
            Span.current().setAttribute( "served_from", "proxy" );

            uni = uni.onItem().invoke( resp -> {
                Object entity = resp.getEntity();
                if ( resp.getStatus() == OK.getStatusCode() && entity instanceof ProxyStreamingOutput )
                {
                    writeToCache( resp, cache, path, (ProxyStreamingOutput) entity );
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

    private Uni<Response> renderCachedFile( CacheConfiguration cache, File cached ) throws IOException
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
        Span.current().setAttribute( "response.content_length", cached.length() );

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

    private void writeToCache( Response resp, CacheConfiguration cache, String path, ProxyStreamingOutput pipe )
    {
        CacheStrategy cacheStrategy = getCacheStrategy( cache.strategy );

        File f = cacheStrategy.getCachedFile( cache, path );
        File metadata = getMetadataFile( f );

        logger.debug( "Write to file: {}", f );
        f.getParentFile().mkdirs();

        try
        {
            //                FileUtils.writeByteArrayToFile( f, bytes );
            FileUtils.writeStringToFile( metadata, getHeadersAsString( resp.getStringHeaders() ), DEFAULT_CHARSET );
            pipe.setCacheStream( new FileOutputStream( f ) );
        }
        catch ( IOException e )
        {
            logger.warn( "Can not write cache file", e );
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
