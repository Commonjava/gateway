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
package org.commonjava.util.gateway.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import kotlin.Pair;
import org.commonjava.util.gateway.cache.CacheHandler;
import org.commonjava.util.gateway.interceptor.ExceptionHandler;
import org.commonjava.util.gateway.util.OtelAdapter;
import org.commonjava.util.gateway.util.ProxyStreamingOutput;
import org.commonjava.util.gateway.util.WebClientAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

import static io.vertx.core.http.HttpMethod.HEAD;
import static io.vertx.core.http.impl.HttpUtils.normalizePath;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.commonjava.util.gateway.services.ProxyConstants.FORBIDDEN_HEADERS;

@ApplicationScoped
@ExceptionHandler
public class ProxyService
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    Classifier classifier;

    @Inject
    CacheHandler cacheHandler;

    @Inject
    OtelAdapter otel;

    public Uni<Response> doHead( String path, HttpServerRequest request ) throws Exception
    {
        return normalizePathAnd( path, p -> classifier.classifyAnd( p, request, ( client, service ) -> wrapAsyncCall(
                        client.head( p, request ).call(), request.method() ) ) );
    }

    public Uni<Response> doGet( String path, HttpServerRequest request ) throws Exception
    {
        return normalizePathAnd( path, p -> classifier.classifyAnd( p, request,
                                                                    ( client, service ) -> cacheHandler.wrapWithCache(
                                                                                    wrapAsyncCall( client.get( p, request )
                                                                                                         .call(),
                                                                                                   request.method() ), p,
                                                                                    service ) ) );
    }

    public Uni<Response> doPost( String path, InputStream is, HttpServerRequest request ) throws Exception
    {
        return normalizePathAnd( path, p -> classifier.classifyAnd( p, request, ( client, service ) -> wrapAsyncCall(
                        client.post( p, is, request )
                              .call(), request.method() ) ) );
    }

    public Uni<Response> doPut( String path, InputStream is, HttpServerRequest request ) throws Exception
    {
        return normalizePathAnd( path, p -> classifier.classifyAnd( p, request, ( client, service ) -> wrapAsyncCall(
                        client.put( p, is, request )
                              .call(), request.method() ) ) );
    }

    public Uni<Response> doDelete( String path, HttpServerRequest request ) throws Exception
    {
        return normalizePathAnd( path, p -> classifier.classifyAnd( p, request, ( client, service ) -> wrapAsyncCall(
                        client.delete( p )
                              .headersFrom( request )
                              .call(), request.method() ) ) );
    }

    private Uni<Response> wrapAsyncCall( WebClientAdapter.CallAdapter asyncCall, HttpMethod method )
    {
        Uni<Response> ret = asyncCall.enqueue()
                                             .onItem()
                                             .transform( ( resp ) -> convertProxyResp( resp, method ) );
        return ret.onFailure().recoverWithItem( this::handleProxyException );
    }

    /**
     * Send status 500 with error message body.
     * @param t error
     */
    private Response handleProxyException( Throwable t )
    {
        logger.error( "Proxy error", t );
        return Response.status( INTERNAL_SERVER_ERROR ).entity( t + ". Caused by: " + t.getCause() ).build();
    }

    /**
     * Read status and headers from proxy resp and set them to direct response.
     * @param resp proxy resp
     * @param method request method
     */
    private Response convertProxyResp( okhttp3.Response resp, HttpMethod method )
    {
        logger.debug( "Proxy resp: {} {}", resp.code(), resp.message() );
        logger.trace( "Raw resp headers:\n{}", resp.headers() );
        Response.ResponseBuilder builder = Response.status( resp.code(), resp.message() );
        resp.headers().forEach( header -> {
            if ( isHeaderAllowed( header, method ) )
            {
                logger.debug( "Setting response header: {} = {}", header.getFirst(), header.getSecond() );
                builder.header( header.getFirst(), header.getSecond() );
            }
        } );

        builder.entity( new ProxyStreamingOutput( resp.body().byteStream(), otel ) );
        return builder.build();
    }

    /**
     * Raw content-length/connection header breaks http2 protocol. Exclude them and let lower layer regenerate it.
     * Allow all headers when it is HEAD request.
     */
    private boolean isHeaderAllowed( Pair<? extends String, ? extends String> header, HttpMethod method )
    {
        if ( method == HEAD )
        {
            return true;
        }
        String key = header.getFirst();
        return !FORBIDDEN_HEADERS.contains( key.toLowerCase() );
    }

    @FunctionalInterface
    private interface CheckedFunction<T, R>
    {
        R apply( T t ) throws Exception;
    }

    private <R> R normalizePathAnd( String path, CheckedFunction<String, R> action ) throws Exception
    {
        return action.apply( normalizePath( path ) );
    }

}
