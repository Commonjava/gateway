package org.commonjava.util.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.commons.io.FileUtils;
import org.commonjava.util.gateway.fixture.WireServerInject;
import org.commonjava.util.gateway.fixture.TestResources;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static java.lang.Thread.sleep;
import static org.commonjava.util.gateway.cache.strategy.DefaultCacheStrategy.DEFAULT_CACHE_DIR;
import static org.commonjava.util.gateway.fixture.TestResources.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTestResource( TestResources.class )
@QuarkusTest
public class ProxyCacheTest
{
    @WireServerInject
    WireMockServer wireMockServer;

    @Test
    public void testProxyGet()
    {
        given().when()
               .get( METADATA_PATH )
               .then()
               .statusCode( 200 )
               .body( is( METADATA_CONTENT ) );

        assertFalse( new File( DEFAULT_CACHE_DIR, METADATA_PATH ).exists() );
    }

    @Test
    public void testProxyGet404()
    {
        given().when()
               .get( NON_EXIST_PATH )
               .then()
               .statusCode( 404 );

        assertFalse( new File( DEFAULT_CACHE_DIR, NON_EXIST_PATH ).exists() );
    }

    @Test
    public void testProxyGetJar()
    {
        given().when()
               .get( JAR_PATH )
               .then()
               .statusCode( 200 )
               .body( notNullValue() );

        assertTrue( new File( DEFAULT_CACHE_DIR, JAR_PATH ).exists() );
    }

    @Test
    public void testProxyGetPom()
    {
        given().when()
               .get( POM_PATH )
               .then()
               .statusCode( 200 )
               .body( is( POM_CONTENT ) );

        assertTrue( new File( DEFAULT_CACHE_DIR, POM_PATH ).exists() );
    }

    @Test
    public void testProxyCacheExpire() throws Exception
    {
        // 1. init request which sets the cached file
        testProxyGetPom();

        // 2. remove stub
        wireMockServer.removeStub( get( urlEqualTo( POM_PATH ) ) );

        // 3. wait 1s and request again, it should get the cached file
        sleep( 1000 );
        testProxyGetPom();

        // 4. wait 1+2=3s and the cached file should have expired (expire is 2s)
        sleep( 2000 );
        given().when()
               .get( POM_PATH )
               .then()
               .statusCode( 404 );

        assertFalse( new File( DEFAULT_CACHE_DIR, POM_PATH ).exists() );
    }

    @AfterAll
    public static void cleanUp()
    {
        try
        {
            FileUtils.deleteDirectory( DEFAULT_CACHE_DIR );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }
}