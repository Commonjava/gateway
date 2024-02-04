/**
 * Copyright (C) 2022 Red Hat, Inc. (https://github.com/Commonjava/service-parent)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.util.gateway.fixture;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.nio.charset.Charset.defaultCharset;
import static org.commonjava.util.gateway.fixture.TestResponseTransformer.TRANSFORMER_NAME;

public class TestResources
                implements QuarkusTestResourceLifecycleManager
{
    public static final String P_BASE_PATH = "/api/content/maven/group/public/org/commonjava/util/o11yphant-metrics-api";

    public static final String METADATA_PATH = P_BASE_PATH + "/maven-metadata.xml";

    public static final String METADATA_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<metadata>\n"
                    + "  <groupId>org.commonjava.util</groupId>\n"
                    + "  <artifactId>o11yphant-metrics-api</artifactId>\n"
                    + "  <versioning>\n"
                    + "    <latest>1.0</latest>\n"
                    + "    <versions>\n"
                    + "      <version>1.0</version>\n"
                    + "    </versions>\n"
                    + "  </versioning>\n"
                    + "</metadata>\n";

    public static final String POM_PATH = P_BASE_PATH + "/1.0/o11yphant-metrics-api-1.0.pom";

    public static final String POM_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<project>\n"
                    + "  <modelVersion>4.0.0</modelVersion>\n"
                    + "  <groupId>org.commonjava.util</groupId>\n"
                    + "  <artifactId>o11yphant-metrics-api</artifactId>\n"
                    + "  <version>1.0</version>"
                    + "</project>\n";

    public static final String NON_EXIST_PATH = "/api/content/maven/hosted/local-deployments/no/such/path";

    public static final String SERVICE_NOT_FOUND_PATH = "/api/none/something";

    public static final String BYTE_FILE_PATH = "/api/content/byte/file";

    public static final byte[] BYTE_CONTENT = new byte[] { 0x01, 0x02, 0x03, 0x04 };

    public static final String JAR_PATH = P_BASE_PATH + "/1.0/o11yphant-metrics-api-1.0.jar";

    public static final byte[] JAR_CONTENT = BYTE_CONTENT;

    public static final String ORIGIN = "Origin";

    public static final String ORIGIN_VALUE = "maven:hosted:local-deployments";

    public static final String PUT_PATH = "/api/content/maven/hosted/local-deployments/my/test/test-1.txt";

    public static final String LARGE_FILE_PATH = "/api/content/maven/hosted/local-deployments/my/test/large.zip";

    public static final String POST_PATH = "/api/admin/stores/maven/hosted";

    public static final String PROMOTE_TIMEOUT_PATH = "/api/other/really/big/promote";

    public static final String PROMOTE_PATH = "/api/promote";

    public static final String EXCEPTION_PATH = "/api/content/maven/hosted/local-deployments/exception";

    public static final String PATH_WITH_REQ_PARAMS_BASE = "/api/admin/stores/query/all";

    public static final String PATH_WITH_REQ_PARAMS_CONTENT = "HELLO";

    public WireMockServer wireMockServer;

    @Override
    public Map<String, String> start()
    {
        wireMockServer = new WireMockServer(
                        new WireMockConfiguration().port( 9090 ).extensions( TestResponseTransformer.class ) );

        wireMockServer.start();

        UrlPattern metadataUrl = urlEqualTo( METADATA_PATH );

        wireMockServer.stubFor( get( metadataUrl ).willReturn(
                        aResponse().withHeader( ORIGIN, ORIGIN_VALUE ).withBody( METADATA_CONTENT ) ) );

        wireMockServer.stubFor( get( urlPathEqualTo(PATH_WITH_REQ_PARAMS_BASE) )
                .withQueryParam( "pkgType", equalTo("maven") )
                .withQueryParam( "type", equalTo("remote") )
                .withQueryParam( "name", equalTo( "builds-untested+shared-imports" ))
                .willReturn( aResponse().withBody( PATH_WITH_REQ_PARAMS_CONTENT )) );

        wireMockServer.stubFor( head( metadataUrl ).willReturn( aResponse().withHeader( ORIGIN, ORIGIN_VALUE ) ) );

        wireMockServer.stubFor(
                        get( urlEqualTo( BYTE_FILE_PATH ) ).willReturn( aResponse().withBody( BYTE_CONTENT ) ) );

        wireMockServer.stubFor(
                        get( urlEqualTo( JAR_PATH ) ).willReturn( aResponse().withBody( JAR_CONTENT ) ) );

        wireMockServer.stubFor(
                        get( urlEqualTo( POM_PATH ) ).willReturn( aResponse().withBody( POM_CONTENT ) ) );

        wireMockServer.stubFor( put( PUT_PATH ).willReturn( aResponse().withStatus( 201 ) ) );

        wireMockServer.stubFor( post( POST_PATH ).willReturn( aResponse().withTransformers( TRANSFORMER_NAME ) ));

        wireMockServer.stubFor( put( LARGE_FILE_PATH ).willReturn( aResponse().withStatus( 201 ) ) );

        wireMockServer.stubFor( post( LARGE_FILE_PATH ).willReturn( aResponse().withStatus( 201 ) ));

        wireMockServer.stubFor( post( PROMOTE_PATH ).willReturn( aResponse().withFixedDelay( 1500 ).withStatus( 200 ) ) );

        wireMockServer.stubFor( post( PROMOTE_TIMEOUT_PATH ).willReturn( aResponse().withFixedDelay( 3000 ).withStatus( 200 ) ) );

        wireMockServer.stubFor( post( EXCEPTION_PATH ).willReturn(
                        aResponse().withFault( Fault.MALFORMED_RESPONSE_CHUNK ) ) );

        return Collections.EMPTY_MAP;
    }

    /**
     * Allow this test resource to provide custom injection of fields of the test class.
     */
    @Override
    public void inject( Object testInstance )
    {
        final Logger logger = LoggerFactory.getLogger( getClass() );
        //logger.debug( "Start injection" );
        Class<?> c = testInstance.getClass();
        while ( c != Object.class )
        {
            //logger.debug( "Check class: {}", c );
            for ( Field f : c.getDeclaredFields() )
            {
                //logger.debug( "Check field: {}", f );
                if ( f.getAnnotation( WireServerInject.class ) != null )
                {
                    //logger.debug( "Get annotated field: {}", f );
                    f.setAccessible( true );
                    try
                    {
                        logger.debug( "Set mock server, {}", wireMockServer );
                        f.set( testInstance, wireMockServer );
                        return;
                    }
                    catch ( Exception e )
                    {
                        throw new RuntimeException( e );
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    @Override
    public void stop()
    {
        if ( wireMockServer != null )
        {
            wireMockServer.stop();
        }
    }
}