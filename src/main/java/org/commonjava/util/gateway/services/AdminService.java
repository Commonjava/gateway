package org.commonjava.util.gateway.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import org.commonjava.util.gateway.config.ProxyConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class AdminService
{
    @Inject
    ProxyConfiguration serviceConfiguration;

    public Uni<JsonObject> getProxyConfig()
    {
        return Uni.createFrom().item( JsonObject.mapFrom( serviceConfiguration ) );
    }
}
