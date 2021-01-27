package org.commonjava.util.gateway.exception.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
