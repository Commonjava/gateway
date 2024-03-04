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
package org.commonjava.util.gateway.metrics.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;

import static org.eclipse.microprofile.health.HealthCheckResponse.Status.UP;

@Liveness
@ApplicationScoped
public class ThreadsHealthCheck
                implements HealthCheck
{
    @Inject
    JvmHealthInfo jvmInfo;

    @Override
    public HealthCheckResponse call()
    {
        return new HealthCheckResponse( "threads", UP,
                                        Optional.of( jvmInfo.getThreadsInfo() ) );
    }
}