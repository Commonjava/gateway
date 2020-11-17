package org.commonjava.util.gateway;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class ProxyResourceTest
{
    @Test
    public void testProxyGet()
    {
        given().when()
               .get( "/api/content/maven/hosted/local-deployments/org/commonjava/util/partyline/maven-metadata.xml" )
               .then()
               .statusCode( 200 )
               .body( containsString( "<artifactId>partyline</artifactId>" ) );
    }

    @Test
    public void testProxyHead()
    {
        given().when()
               .head( "/api/content/maven/hosted/local-deployments/org/commonjava/util/partyline/maven-metadata.xml" )
               .then()
               .statusCode( 200 )
               .header( "Indy-Cur-API-Version", is( "1" ) )
               .header( "Indy-Origin", is( "maven:hosted:local-deployments" ) );
    }

    @Test
    public void testProxyHead404()
    {
        given().when()
               .head( "/api/content/maven/hosted/local-deployments/no/such/path" )
               .then()
               .statusCode( 404 );
    }

    @Test
    public void testProxyGetBytes()
    {
        given().when()
               .get( "/api/content/maven/hosted/local-deployments/org/commonjava/util/partyline/2.1-SNAPSHOT/partyline-2.1-20191014.214930-1.jar" )
               .then()
               .statusCode( 200 )
               .body( is( notNullValue() ) );
    }

    @Test
    public void testProxyPost()
    {
        /* @formatter:off */
        String body = "{"
                        + "  \"key\": \"maven:hosted:local-1\","
                        + "  \"type\": \"hosted\","
                        + "  \"packageType\": \"maven\","
                        + "  \"name\": \"local-1\""
                        + "}";
        /* @formatter:on */
        given().when()
               .body( body )
               .post( "/api/admin/stores/maven/hosted" )
               .then()
               .statusCode( 200 )
               .body( containsString( "create_time" ) );
    }

    @Test
    public void testProxyPut()
    {
        given().when()
               .body( "This is a test " + new Date() )
               .put( "/api/content/maven/hosted/local-1/my/test/test-1.txt" )
               .then()
               .statusCode( 204 );
    }

}