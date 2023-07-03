/**
 * Copyright (C) 2022 John Casey (jdcasey@commonjava.org)
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
import org.commonjava.util.gateway.metrics.response.component.ResponseCount;
import org.commonjava.util.gateway.services.ProxyService;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static org.commonjava.util.gateway.util.ServiceUtils.pathWithParams;

@Path( "/{path: (.*)}" )
public class ProxyResource
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    ProxyService proxyService;

    @HEAD
    @ResponseCount
    public Uni<Response> head( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
                    throws Exception
    {
        logger.debug( "Head resource: {}", path );
        return proxyService.doHead( path, request );
    }

    @GET
    @ResponseCount
    public Uni<Response> get( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
                    throws Exception
    {
        logger.debug( "Get resource: {}, query: {}", path, request.query() );
        return proxyService.doGet( pathWithParams( path, request.query() ), request );
    }

    @POST
    @ResponseCount
    public Uni<Response> post( @PathParam( "path" ) String path, InputStream is,
                               final @Context HttpServerRequest request ) throws Exception
    {
        logger.debug( "Post resource: {}", path );
        return proxyService.doPost( path, is, request );
    }

    @PUT
    @ResponseCount
    public Uni<Response> put( @PathParam( "path" ) String path, InputStream is,
                              final @Context HttpServerRequest request ) throws Exception
    {
        logger.debug( "Put resource: {}", path );
        return proxyService.doPut( path, is, request );
    }

    @DELETE
    @ResponseCount
    public Uni<Response> delete( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
                    throws Exception
    {
        logger.debug( "Delete resource: {}", path );
        return proxyService.doDelete( path, request );
    }

}
