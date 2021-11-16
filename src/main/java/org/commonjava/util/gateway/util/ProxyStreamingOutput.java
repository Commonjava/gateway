package org.commonjava.util.gateway.util;

import io.opentelemetry.api.trace.Span;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.checkerframework.checker.units.qual.C;
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

    private OtelAdapter otel;

    public ProxyStreamingOutput( InputStream bodyStream, OtelAdapter otel )
    {
        this.bodyStream = bodyStream;
        this.otel = otel;
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
                    out = new TeeOutputStream( output, cacheStream ); // lgtm [java/output-resource-leak]
                }
                CountingOutputStream cout = new CountingOutputStream( out );
                out = cout;

                logger.trace( "Copying from: {} to: {}", bodyStream, out );
                IOUtils.copy( bodyStream, out );

                if ( otel.enabled() )
                {
                    Span.current().setAttribute( "response.content_length", cout.getByteCount() );
                }

            }
            finally
            {
                closeBodyStream( bodyStream );
                closeCacheStream( cacheStream );
            }
        }
        else
        {
            if ( otel.enabled() )
            {
                Span.current().setAttribute( "response.content_length", 0 );
            }
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
            if ( otel.enabled() )
            {
                Span.current().setAttribute( "body.ignored_error_class", e.getClass().getSimpleName() );
                Span.current().setAttribute( "body.ignored_error_class", e.getMessage() );
            }

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
            if ( otel.enabled() )
            {
                Span.current().setAttribute( "cache.ignored_error_class", e.getClass().getSimpleName() );
                Span.current().setAttribute( "cache.ignored_error_class", e.getMessage() );
            }

            logger.trace( "Failed to close cache stream in proxy response.", e );
        }
    }

}
