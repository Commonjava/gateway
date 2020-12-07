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
@ProxyExceptionHandler
public class ProxyExceptionHandlerInterceptor
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @AroundInvoke
    public Object handleException( InvocationContext invocationContext ) throws Exception
    {
        try
        {
            return invocationContext.proceed();
        }
        catch ( ServiceNotFoundException e )
        {
            return Uni.createFrom().item( Response.status( 400 ).entity( e.getMessage() ).build() );
        }
    }
}
