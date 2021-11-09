package org.commonjava.util.gateway.util;

import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Supplier;

public class BufferStreamingOutput
                implements StreamingOutput
{
    private static final int bufSize = 10 * 1024 * 1024;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private HttpResponse<Buffer> response;

    private Supplier<OutputStream> cacheStreamSupplier;

    public BufferStreamingOutput( HttpResponse<Buffer> response )
    {
        this.response = response;
    }

    @Override
    public void write( OutputStream output ) throws IOException, WebApplicationException
    {
        OutputStream cacheStream = null;
        try
        {
            OutputStream out = output;
            if ( cacheStreamSupplier != null )
            {
                cacheStream = cacheStreamSupplier.get();
                if ( cacheStream != null )
                {
                    out = new TeeOutputStream( cacheStream, output );
                }
            }

            Buffer buffer = response.bodyAsBuffer();
            int total = buffer.length();
            //        logger.info( "Writing {} bytes", total );
            int transferred = 0;
            while ( transferred < total )
            {
                int next = bufSize < total ? bufSize : total;
                out.write( buffer.getBytes( transferred, next ) );
                //            logger.info( "Wrote to index: {}", next );
                transferred = next;
            }
            out.flush();
        }
        finally
        {
            if ( cacheStream != null )
            {
                try
                {
                    cacheStream.close();
                }
                catch ( Exception e )
                {
                    logger.error( "Failed to close cache stream: " + e.getMessage(), e );
                }
            }
        }
    }

    public void setCacheStreamSupplier( Supplier<OutputStream> cacheStreamSupplier )
    {
        this.cacheStreamSupplier = cacheStreamSupplier;
    }
}
