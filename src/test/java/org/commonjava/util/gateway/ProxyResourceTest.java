package org.commonjava.util.gateway;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;

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

}