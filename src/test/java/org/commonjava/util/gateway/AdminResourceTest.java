package org.commonjava.util.gateway;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class AdminResourceTest
{
    @Test
    public void testProxyConfig() throws IOException
    {
        given().when().get( "/proxy/config" ).then().statusCode( 200 ).body( containsString( "services" ) );
    }

}