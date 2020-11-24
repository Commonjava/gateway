package org.commonjava.util.gateway;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
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

@Path( "/api/{path: (.*)}" )
public class ProxyResource
{
    private static final String API_ROOT = "/api";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ProxyService proxyService;

    @HEAD
    public Uni<Response> head( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
                    throws Exception
    {
        logger.debug( "Head resource: {}", path );
        return proxyService.doHead( API_ROOT + "/" + path, request );
    }

    @GET
    public Uni<Response> get( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
                    throws Exception
    {
        logger.debug( "Get resource: {}", path );
        return proxyService.doGet( API_ROOT + "/" + path, request );
    }

    @POST
    public Uni<Response> post( @PathParam( "path" ) String path, InputStream is,
                               final @Context HttpServerRequest request ) throws Exception
    {
        logger.debug( "Post resource: {}", path );
        return proxyService.doPost( API_ROOT + "/" + path, is, request );
    }

    @PUT
    public Uni<Response> put( @PathParam( "path" ) String path, InputStream is,
                              final @Context HttpServerRequest request ) throws Exception
    {
        logger.debug( "Put resource: {}", path );
        return proxyService.doPut( API_ROOT + "/" + path, is, request );
    }

    @DELETE
    public Uni<Response> delete( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
                    throws Exception
    {
        logger.debug( "Delete resource: {}", path );
        return proxyService.doDelete( API_ROOT + "/" + path, request );
    }

}
