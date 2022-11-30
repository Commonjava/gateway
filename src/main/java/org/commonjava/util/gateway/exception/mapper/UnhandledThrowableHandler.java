/**
 * Copyright (C) 2020 John Casey (jdcasey@commonjava.org)
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
package org.commonjava.util.gateway.exception.mapper;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * This default mapper allows any exception to be captured ensuring a ResponseContextFilter is always called.
 */
@Provider
public class UnhandledThrowableHandler
        implements ExceptionMapper<Throwable>//, RestProvider
{
    public Response toResponse( Throwable exception )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.error( "Unhandled exception: " + exception.getMessage(), exception );

        return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
                       .entity( ExceptionUtils.getStackTrace( exception ) )
                       .type( MediaType.TEXT_PLAIN )
                       .build();
    }
}
