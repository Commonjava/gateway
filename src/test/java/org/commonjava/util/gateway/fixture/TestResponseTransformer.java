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