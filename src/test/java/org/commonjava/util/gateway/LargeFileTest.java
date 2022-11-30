/**
 * Copyright (C) 2022 John Casey (jdcasey@commonjava.org)
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