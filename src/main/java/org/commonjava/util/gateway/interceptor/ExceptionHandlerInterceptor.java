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
