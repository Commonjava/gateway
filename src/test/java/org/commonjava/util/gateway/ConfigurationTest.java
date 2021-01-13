package org.commonjava.util.gateway;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ConfigurationTest
{
    @Test
    public void testProxyConfig() throws IOException
    {
        Response resp = given().when().get( "/proxy/config" );
        String body = resp.getBody().asString();
        assertTrue( body.contains( "services" ) );
        assertTrue( body.indexOf( "test-" ) == body.lastIndexOf( "test-" ) ); // either test-1 or test-2 exists, but not both
    }

}