package org.commonjava.util.gateway.fixture;

import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.commonjava.util.gateway.fixture.TestResponseTransformer.TRANSFORMER_NAME;

public class TestResources
                implements QuarkusTestResourceLifecycleManager
{
    public static final String METADATA_PATH = "/api/content/maven/group/public/org/commonjava/util/o11yphant-metrics-api/maven-metadata.xml";

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

    public static final String NON_EXIST_PATH = "/api/content/maven/hosted/local-deployments/no/such/path";

    public static final String SERVICE_NOT_FOUND_PATH = "/api/none/something";

    public static final String JAR_PATH = "/api/content/maven/group/public/org/commonjava/util/o11yphant-metrics-api/1.0/o11yphant-metrics-api-1.0.jar";

    public static final byte[] JAR_CONTENT = new byte[] { 0x01, 0x02, 0x03, 0x04 };

    public static final String ORIGIN = "Origin";

    public static final String ORIGIN_VALUE = "maven:hosted:local-deployments";

    public static final String PUT_PATH = "/api/content/maven/hosted/local-deployments/my/test/test-1.txt";

    public static final String LARGE_JAR_PATH = "/api/content/maven/hosted/local-deployments/my/test/large.jar";

    public static final String POST_PATH = "/api/admin/stores/maven/hosted";

    private WireMockServer wireMockServer;

    @Override
    public Map<String, String> start()
    {
        wireMockServer = new WireMockServer(
                        new WireMockConfiguration().port( 9090 ).extensions( TestResponseTransformer.class ) );

        wireMockServer.start();

        UrlPattern metadataUrl = urlEqualTo( METADATA_PATH );

        wireMockServer.stubFor( get( metadataUrl ).willReturn(
                        aResponse().withHeader( ORIGIN, ORIGIN_VALUE ).withBody( METADATA_CONTENT ) ) );

        wireMockServer.stubFor( head( metadataUrl ).willReturn( aResponse().withHeader( ORIGIN, ORIGIN_VALUE ) ) );

        wireMockServer.stubFor(
                        get( urlEqualTo( JAR_PATH ) ).willReturn( aResponse().withBody( JAR_CONTENT ) ) );

        wireMockServer.stubFor( put( PUT_PATH ).willReturn( aResponse().withStatus( 201 ) ) );

        wireMockServer.stubFor( post( POST_PATH ).willReturn( aResponse().withTransformers( TRANSFORMER_NAME ) ));

        wireMockServer.stubFor( put( LARGE_JAR_PATH ).willReturn( aResponse().withStatus( 201 ) ) );

        wireMockServer.stubFor( post( LARGE_JAR_PATH ).willReturn( aResponse().withStatus( 201 ) ));

        return Collections.EMPTY_MAP;
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