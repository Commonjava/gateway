package org.commonjava.util.gateway.services;

import java.util.Arrays;
import java.util.List;

public class ProxyConstants
{
    // Vert.x event types
    public static final String EVENT_PROXY_CONFIG_CHANGE = "proxy-config-change";

    // Auto generated, ignore such upstream headers
    public static final List<String> FORBIDDEN_HEADERS =
                    Arrays.asList( "connection", "transfer-encoding" );
}
