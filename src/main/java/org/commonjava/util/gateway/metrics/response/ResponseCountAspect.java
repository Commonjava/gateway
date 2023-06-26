package org.commonjava.util.gateway.metrics.response;

import org.commonjava.util.gateway.metrics.response.component.ResponseCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@ResponseCount
@Interceptor
public class ResponseCountAspect {
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @AroundInvoke
    Object markInvocation(InvocationContext context) {
        //context包含调用点信息比如method paramter 之类的
        logger.debug("@@@@@@ start count");
        for (Object o : context.getParameters()) {
            logger.debug("###### Parameters: {}", o.toString());
        }
        Object ret = null;
        try {
            ret = context.proceed();
        } catch (Exception e) {
            logger.error(e.toString());
        }
        logger.debug("@@@@@@ count end");
        return ret;
    }
}
