package io.github.jonestimd.finance.yahoo.annotation;

import java.math.BigDecimal;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JsonTypeTest {
    public static final String PROPERTY = "property";
    @Mock
    private JsonObject jsonObject;

    @Test
    public void stringGetterReturnsPropertyValue() throws Exception {
        when(jsonObject.getString(PROPERTY)).thenReturn("value");

        assertThat(JsonType.STRING.get(jsonObject, PROPERTY)).isEqualTo("value");
    }

    @Test
    public void bigDecimalGetterReturnsNull() throws Exception {
        when(jsonObject.isNull(PROPERTY)).thenReturn(true);

        assertThat(JsonType.BIG_DECIMAL.get(jsonObject, PROPERTY)).isNull();
    }

    @Test
    public void bigDecimalGetterConvertsValue() throws Exception {
        when(jsonObject.isNull(PROPERTY)).thenReturn(false);
        when(jsonObject.getString(PROPERTY)).thenReturn("123.00");

        assertThat(JsonType.BIG_DECIMAL.get(jsonObject, PROPERTY)).isEqualTo(new BigDecimal("123.00"));
    }
}