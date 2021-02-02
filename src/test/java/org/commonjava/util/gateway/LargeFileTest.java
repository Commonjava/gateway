package org.commonjava.util.gateway;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.commonjava.util.gateway.fixture.TestResources;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.commonjava.util.gateway.fixture.TestResources.LARGE_FILE_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.anyOf;

@QuarkusTestResource( TestResources.class )
@QuarkusTest
public class LargeFileTest
{
    private int SIZE_20 = 1024 * 1024 * 20; // 20M

    @Test
    public void testProxyPost() throws IOException
    {
        given().when()
               .body( getBytes( SIZE_20 ) )
               .post( LARGE_FILE_PATH )
               .then()
               .statusCode( anyOf( is( 201 ), is( 204 ) ) );
    }

    @Test
    public void testProxyPut() throws IOException
    {
        given().when()
               .body( getBytes( SIZE_20 ) )
               .put( LARGE_FILE_PATH )
               .then()
               .statusCode( anyOf( is( 201 ), is( 204 ) ) );
    }

    private byte[] getBytes( int size )
    {
        byte[] bytes = new byte[size];
        Random rd = new Random();
        rd.nextBytes( bytes );
        return bytes;
    }

}