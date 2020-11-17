package org.commonjava.util.gateway;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.commonjava.util.gateway.services.ProxyService;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import java.io.InputStream;

@Path( "/api/{path: (.*)}" )
public class ProxyResource
{
    private static final String API_ROOT = "/api";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ProxyService proxyService;

    @HEAD
    public void head( @PathParam( "path" ) String path, final @Context HttpServerRequest request,
                      final @Context HttpServerResponse response ) throws Exception
    {
        logger.debug( "Head resource: {}", path );
        proxyService.doHead( API_ROOT + "/" + path, request, response );
    }

    @GET
    public Uni<byte[]> get( @PathParam( "path" ) String path, final @Context HttpServerRequest request,
                            final @Context HttpServerResponse response ) throws Exception
    {
        logger.debug( "Get resource: {}", path );
        return proxyService.doGetBytes( API_ROOT + "/" + path, request, response );
    }

    @POST
    public Uni<byte[]> post( @PathParam( "path" ) String path, InputStream is, final @Context HttpServerRequest request,
                             final @Context HttpServerResponse response ) throws Exception
    {
        logger.debug( "Post resource: {}", path );
        return proxyService.doPostBytes( API_ROOT + "/" + path, is, request, response );
    }

    @PUT
    public Uni<byte[]> put( @PathParam( "path" ) String path, InputStream is, final @Context HttpServerRequest request,
                            final @Context HttpServerResponse response ) throws Exception
    {
        logger.debug( "Put resource: {}", path );
        return proxyService.doPutBytes( API_ROOT + "/" + path, is, request, response );
    }

}
