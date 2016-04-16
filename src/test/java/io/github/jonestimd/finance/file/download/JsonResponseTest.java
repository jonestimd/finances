package io.github.jonestimd.finance.file.download;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.http.HttpEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JsonResponseTest {
    @Mock
    private HttpEntity entity;

    @Test
    public void getValueReturnsString() throws Exception {
        Config config = ConfigFactory.parseString("{selector = field}");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("{\"field\":\"value\"}".getBytes("UTF-8")));
        JsonResponse response = new JsonResponse(entity);

        Object value = response.getValue(config);

        assertThat(value).isEqualTo("value");
    }

    @Test
    public void getValueReturnsNumberAsString() throws Exception {
        Config config = ConfigFactory.parseString("{selector = field}");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("{\"field\":123}".getBytes("UTF-8")));
        JsonResponse response = new JsonResponse(entity);

        Object value = response.getValue(config);

        assertThat(value).isEqualTo("123");
    }

    @Test
    public void getValueReturnsBooleanAsString() throws Exception {
        Config config = ConfigFactory.parseString("{selector = field}");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("{\"field\":true}".getBytes("UTF-8")));
        JsonResponse response = new JsonResponse(entity);

        Object value = response.getValue(config);

        assertThat(value).isEqualTo("true");
    }

    @Test
    public void getValueParsesDate() throws Exception {
        Config config = ConfigFactory.parseString("{selector = field, type = date, format = \"yyyy-MM-dd\"}");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("{\"field\":\"2016-03-10\"}".getBytes("UTF-8")));
        JsonResponse response = new JsonResponse(entity);

        Object value = response.getValue(config);

        assertThat(value).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2016-03-10"));
    }

    @Test
    public void getValuesReturnsStringArray() throws Exception {
        Config config = ConfigFactory.parseString("{selector.path = field}");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream("{\"field\":[\"value1\",2,true]}".getBytes("UTF-8")));
        JsonResponse response = new JsonResponse(entity);

        List<List<Object>> values = response.getValues(config);

        assertThat(values).hasSize(3);
        assertThat(values.get(0)).containsExactly("value1");
        assertThat(values.get(1)).containsExactly("2");
        assertThat(values.get(2)).containsExactly("true");
    }

    @Test
    public void getValuesReturnsObjectFields() throws Exception {
        Config config = ConfigFactory.parseString("{selector { path = field, fields = [attr1, attr2] }, format = ignored }");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(("{\"field\":[" +
                "{\"attr1\":\"string1\",\"attr2\":1}," +
                "{\"attr1\":\"string2\",\"attr2\":2}]}").getBytes("UTF-8")));
        JsonResponse response = new JsonResponse(entity);

        List<List<Object>> values = response.getValues(config);

        assertThat(values).hasSize(2);
        assertThat(values.get(0)).containsExactly("string1", "1");
        assertThat(values.get(1)).containsExactly("string2", "2");
    }

    @Test
    public void getValuesReturnsTableColumns() throws Exception {
        Config config = ConfigFactory.parseString("{selector { path = field, fields = [column2, column1, column3] }, format = table }");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(("{\"field\":[" +
                "[\"column1\",\"column2\",\"column3\"]," +
                "[\"string1\",1,true]," +
                "[\"string2\",2,false]]}").getBytes("UTF-8")));
        JsonResponse response = new JsonResponse(entity);

        List<List<Object>> values = response.getValues(config);

        assertThat(values).hasSize(2);
        assertThat(values.get(0)).containsExactly("1", "string1", "true");
        assertThat(values.get(1)).containsExactly("2", "string2", "false");
    }
}