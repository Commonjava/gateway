package org.commonjava.util.gateway;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import org.commonjava.util.gateway.services.ProxyService;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.io.InputStream;

@Path( "/api" )
public class ProxyResource
{
    private static final String API_ROOT = "/api";

    private final org.slf4j.Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ProxyService proxyService;

    // TODO: PUT / POST via bytes / stream

    @GET
    @Path( "/{path: (.*)}" )
    public Uni<byte[]> getBytes( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
    {
        logger.debug( "Get resource: {}", path );
        return proxyService.doRequestBytes( API_ROOT + "/" + path, request );
    }

    @POST
    @Path( "/{path: (.*)}" )
    public Uni<byte[]> postBytes( @PathParam( "path" ) String path, InputStream is,
                                  final @Context HttpServerRequest request ) throws IOException
    {
        logger.debug( "Post resource: {}, body: {}", path, is );
        return proxyService.doPostBytes( API_ROOT + "/" + path, is, request );
    }

    @PUT
    @Path( "/{path: (.*)}" )
    public Uni<byte[]> putBytes( @PathParam( "path" ) String path, InputStream is,
                                 final @Context HttpServerRequest request ) throws IOException
    {
        logger.debug( "Put resource: {}", path );
        return proxyService.doPutBytes( API_ROOT + "/" + path, is, request );
    }

}
