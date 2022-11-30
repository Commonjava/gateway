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
package org.commonjava.util.gateway.interceptor;

import io.smallrye.mutiny.Uni;
import org.commonjava.util.gateway.exception.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;

@Interceptor
@ExceptionHandler
public class ExceptionHandlerInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @AroundInvoke
    public Object handle( InvocationContext invocationContext ) throws Exception
    {
        try
        {
            return invocationContext.proceed();
        }
        catch ( ServiceNotFoundException e )
        {
            return replaceWith400( e );
        }
    }

    private Object replaceWith400( ServiceNotFoundException e )
    {
        return Uni.createFrom().item( Response.status( 400 ).entity( e.getMessage() ).build() );
    }

}
