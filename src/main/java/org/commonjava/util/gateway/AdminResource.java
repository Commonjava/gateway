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
import io.vertx.core.json.JsonObject;
import org.commonjava.util.gateway.services.AdminService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

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

    @GET
    @Produces( TEXT_PLAIN )
    @Path( "/threads" )
    public Uni<String> getThreadDump( final @Context HttpServerRequest request )
    {
        return adminService.getThreadDumpString();
    }

}
