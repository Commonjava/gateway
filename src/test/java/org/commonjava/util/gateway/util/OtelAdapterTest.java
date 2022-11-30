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
package org.commonjava.util.gateway.util;

import io.opentelemetry.api.trace.Span;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class OtelAdapterTest {
    OtelAdapter otelAdapter;

    @BeforeEach
    void setUp() {
        otelAdapter = new OtelAdapter();
    }

    @Test
    void shouldReturnNoopSpanWhenDisabled() {
        //Given
        otelAdapter.enabled = false;

        //When
        final Span clientSpan = otelAdapter.newClientSpan("test-adapter", "disabledTest");

        //Then
        assertThat(clientSpan.isRecording(), is(false));
    }

    @Test
    void shouldReturnPropagatableSpanWhenDisabled() {
        //Given
        otelAdapter.enabled = true;

        //When
        final Span clientSpan = otelAdapter.newClientSpan("test-adapter", "disabledTest");

        //Then
        assertThat(clientSpan.isRecording(), is(true));
        assertThat(clientSpan.getSpanContext().getSpanId(), notNullValue(String.class));
    }
}
