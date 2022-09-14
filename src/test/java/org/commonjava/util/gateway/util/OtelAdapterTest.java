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
