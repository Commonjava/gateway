package org.commonjava.util.gateway;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class ProxyResourceTest
{
    @Test
    public void testProxyAsyncEndpoint()
    {
        given().when()
               .get( "/api/content/maven/hosted/local-deployments/org/commonjava/util/partyline/maven-metadata.xml" )
               .then()
               .statusCode( 200 )
               .body( containsString( "<artifactId>partyline</artifactId>" ) );
    }

    @Test
    public void testProxyBytesAsyncEndpoint()
    {
        given().when()
               .get( "/api/content/maven/hosted/local-deployments/org/commonjava/util/partyline/2.1-SNAPSHOT/partyline-2.1-20191014.214930-1.jar" )
               .then()
               .statusCode( 200 )
               .body( is( notNullValue() ) );
    }

}