package org.commonjava.util.gateway.metrics.jfr.events;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name ( JaxRSEvent.NAME )
@Label ( "Invocation" )
@Category ( "JaxRS" )
@Description ( "JaxRS invocation event" )
@StackTrace ( false )
public class JaxRSEvent extends Event
{
    public static final String NAME = "o.c.u.g.m.j.e.JaxRSEvent";

    @Label ( "Resource Method" )
    public String method;

    @Label ( "Media type" )
    public String mediaType;

    @Label ( "Java method" )
    public String methodFrameName;

    @Label ( "Path" )
    public String path;

    @Label ( "Request length" )
    @DataAmount
    public int length;

    @Label ( "Response length" )
    @DataAmount
    public int responseLength;

    @Label ( "Status" )
    public int status;
}
