package org.commonjava.util.gateway.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProxyStreamingOutput
    implements StreamingOutput
{
    private Logger logger = LoggerFactory.getLogger( getClass() );

    private OutputStream cacheStream;

    private InputStream bodyStream;

    public ProxyStreamingOutput( InputStream bodyStream )
    {
        this.bodyStream = bodyStream;
    }

    public void setCacheStream( OutputStream cacheStream )
    {
        this.cacheStream = cacheStream;
    }

    @Override
    public void write( OutputStream output ) throws IOException
    {
        if ( bodyStream != null )
        {
            try
            {
                OutputStream out = output;
                if ( cacheStream != null )
                {
                    out = new TeeOutputStream( output, cacheStream );
                }

                logger.trace( "Copying from: {} to: {}", bodyStream, out );
                IOUtils.copy( bodyStream, out );
            }
            finally
            {
                closeBodyStream( bodyStream );
                closeCacheStream( cacheStream );
            }
        }
        else
        {
            throw new IOException( "No upstream body to copy!" );
        }
    }

    private void closeBodyStream( InputStream is )
    {
        if ( is == null )
        {
            return;
        }

        try
        {
            is.close();
        }
        catch ( IOException e )
        {
            logger.trace( "Failed to close body stream in proxy response.", e );
        }
    }

    private void closeCacheStream( OutputStream os )
    {
        if ( os == null )
        {
            return;
        }

        try
        {
            os.flush();
            os.close();
        }
        catch ( IOException e )
        {
            logger.trace( "Failed to close cache stream in proxy response.", e );
        }
    }

}
