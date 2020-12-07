package org.commonjava.util.gateway;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.commonjava.util.gateway.services.AdminService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path( "/proxy" )
public class AdminResource
{
    @Inject
    AdminService adminService;

    @GET
    @Produces( APPLICATION_JSON )
    @Path( "/config" )
    public Uni<JsonObject> getProxyConfig( final @Context HttpServerRequest request )
    {
        return adminService.getProxyConfig();
    }

    @GET
    @Produces( APPLICATION_JSON )
    @Path( "/info" )
    public Uni<JsonObject> getProxyInfo( final @Context HttpServerRequest request )
    {
        return adminService.getProxyInfo();
    }

}
