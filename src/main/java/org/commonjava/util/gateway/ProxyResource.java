package org.commonjava.util.gateway;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.commonjava.util.gateway.services.Classifier;
import org.commonjava.util.gateway.services.ProxyService;
import org.jboss.resteasy.annotations.jaxrs.PathParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path( "/api" )
public class ProxyResource
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ProxyService proxyService;

    @Inject
    private Classifier classifier;

    // TODO: GET / PUT / POST via bytes stream

    @GET
    @Produces( MediaType.APPLICATION_XML )
    @Path( "/{path: (.*)}" )
    public Uni<String> getData( @PathParam( "path" ) String path, final @Context HttpServerRequest request )
    {
        WebClient client = classifier.getWebClient( path );

        logger.debug( "Get resource: {}", path );
        return proxyService.doRequest( client, "/api/" + path, request );
    }

}
