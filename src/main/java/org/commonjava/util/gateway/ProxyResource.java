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
package org.commonjava.util.gateway;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.*;
import org.commonjava.util.gateway.services.ProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

import static org.commonjava.util.gateway.util.ServiceUtils.pathWithParams;

@Path( "/{path: (.*)}" )
public class ProxyResource
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    ProxyService proxyService;

    @HEAD
    public Uni<Response> head(@PathParam( "path" ) String path, final @Context HttpServerRequest request )
                    throws Exception
    {
        logger.debug( "Head resource: {}", path );
        return proxyService.doHead( path, request );
    }

    @GET
    public Uni<Response> get( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
                    throws Exception
    {
        logger.debug( "Get resource: {}, query: {}", path, request.query() );
        return proxyService.doGet( pathWithParams( path, request.query() ), request );
    }

    @POST
    public Uni<Response> post( @PathParam( "path" ) String path, InputStream is,
                               final @Context HttpServerRequest request ) throws Exception
    {
        logger.debug( "Post resource: {}", path );
        return proxyService.doPost( path, is, request );
    }

    @PUT
    public Uni<Response> put( @PathParam( "path" ) String path, InputStream is,
                              final @Context HttpServerRequest request ) throws Exception
    {
        logger.debug( "Put resource: {}", path );
        return proxyService.doPut( path, is, request );
    }

    @DELETE
    public Uni<Response> delete( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
                    throws Exception
    {
        logger.debug( "Delete resource: {}", path );
        return proxyService.doDelete( path, request );
    }

}
