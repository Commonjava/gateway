/**
 * Copyright (C) 2020 John Casey (jdcasey@commonjava.org)
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

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

public class TestResponseTransformer
                extends ResponseDefinitionTransformer
{
    public static final String TRANSFORMER_NAME = "return-same-body-transformer";

    @Override
    public ResponseDefinition transform( Request request, ResponseDefinition responseDefinition, FileSource files,
                                         Parameters parameters )
    {
        return new ResponseDefinitionBuilder().withStatus( 200 ).withBody( request.getBodyAsString() ).build();
    }

    @Override
    public String getName()
    {
        return TRANSFORMER_NAME;
    }

    @Override
    public boolean applyGlobally()
    {
        return false;
    }

}