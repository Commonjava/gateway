package org.commonjava.util.gateway;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import org.commonjava.util.gateway.services.ProxyService;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

@Path( "/api" )
public class ProxyResource
{
    private final org.slf4j.Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ProxyService proxyService;

    // TODO: PUT / POST via bytes / stream

    @GET
    @Path( "/{path: (.*)}" )
    public Uni<byte[]> getBytes( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
    {
        logger.debug( "Get resource: {}", path );
        return proxyService.doRequestBytes( "/api/" + path, request );
    }

}
