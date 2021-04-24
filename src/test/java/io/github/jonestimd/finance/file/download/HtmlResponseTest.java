package io.github.jonestimd.finance.file.download;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.http.HttpEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HtmlResponseTest {
    private static final String BASE_URL = "/";
    private static final String DATE = "2016-03-10";
    private final String html = "<html><body>" +
            "<div><h4 id=\"id2\">Heading 1</h4><h4 id=\"id1\">Heading 2</h4></div>" +
            "<span>" + DATE + "</span>" +
            "<span>$1234.56</span>" +
            "</body></html>";
    @Mock
    private HttpEntity entity;

    @Test
    public void getValueReturnsContainedText() throws Exception {
        Config config = ConfigFactory.parseString("{selector = div }");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        Object value = response.getValue(config);

        assertThat(value).isEqualTo("Heading 1 Heading 2");
    }

    @Test
    public void getValueReturnsElementText() throws Exception {
        Config config = ConfigFactory.parseString("{selector = \"h4/ownText()\" }");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        Object value = response.getValue(config);

        assertThat(value).isEqualTo("Heading 1");
    }

    @Test
    public void getValueReturnsElementAttribute() throws Exception {
        Config config = ConfigFactory.parseString("{selector = \"h4/@id\", type = string }");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        Object value = response.getValue(config);

        assertThat(value).isEqualTo("id2");
    }

    @Test
    public void getValueParsesDate() throws Exception {
        Config config = ConfigFactory.parseString("{selector = span, type = date, format = yyyy-MM-dd}");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        Object value = response.getValue(config);

        assertThat(value).isInstanceOf(Date.class);
        assertThat(value).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse(DATE));
    }

    @Test
    public void getValueParsesNumber() throws Exception {
        Config config = ConfigFactory.parseString("{selector = \"span:eq(2)\", type = number, format = \"$####.##\"}");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        Object value = response.getValue(config);

        assertThat(value).isInstanceOf(Number.class);
        assertThat(value).isEqualTo(1234.56);
    }

    @Test
    public void getValueThrowsExceptionForInvalidType() throws Exception {
        Config config = ConfigFactory.parseString("{selector = span, type = xml}");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        try {
            response.getValue(config);
            fail("expected exception");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("Invalid type: xml");
        }
    }

    @Test
    public void getValueReturnsEmptyStringForUnsetAttribute() throws Exception {
        Config config = ConfigFactory.parseString("{selector = \"h4/@key\" }");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        Object value = response.getValue(config);

        assertThat(value).isEqualTo("");
    }

    @Test
    public void getValueReturnsNullForNoMatchingElement() throws Exception {
        Config config = ConfigFactory.parseString("{selector = \"h3/@id\" }");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        Object value = response.getValue(config);

        assertThat(value).isNull();
    }

    @Test
    public void getValueThrowsExceptionForUnknownAccessor() throws Exception {
        Config config = ConfigFactory.parseString("{selector = \"h4/unknown()\" }");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        try {
            Object value = response.getValue(config);
            fail("Expected an exception");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("Unknown selector: unknown()");
        }
    }

    @Test
    public void getValueThrowsExceptionForInvalidAccessor() throws Exception {
        Config config = ConfigFactory.parseString("{selector = \"h4/getClass()\" }");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        try {
            response.getValue(config);
            fail("Expected an exception");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("Unknown selector: getClass()");
        }
    }

    @Test
    public void getValuesReturnsElementTextByDefault() throws Exception {
        Config config = ConfigFactory.parseString("{selector.path = h4 }");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        List<List<Object>> values = response.getValues(config);

        assertThat(values).hasSize(2);
        assertThat(values.get(0)).containsExactly("Heading 1");
        assertThat(values.get(1)).containsExactly("Heading 2");
    }

    @Test
    public void getValuesReturnsElementText() throws Exception {
        Config config = ConfigFactory.parseString("{selector { path = h4, fields = [ \"@id\", \"text()\" ] }}");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        List<List<Object>> values = response.getValues(config);

        assertThat(values).hasSize(2);
        assertThat(values.get(0)).containsExactly("id2", "Heading 1");
        assertThat(values.get(1)).containsExactly("id1", "Heading 2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getValuesThrowsExceptionForInvalidField() throws Exception {
        Config config = ConfigFactory.parseString("{selector { path = h4, fields = [ \"@id\", \"text\" ] }}");
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(html.getBytes("UTF-8")));
        HtmlResponse response = new HtmlResponse(entity, BASE_URL);

        response.getValues(config);
    }
}