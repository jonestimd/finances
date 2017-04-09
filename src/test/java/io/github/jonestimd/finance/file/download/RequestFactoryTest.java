package io.github.jonestimd.finance.file.download;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.jonestimd.util.Streams;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;

import static com.typesafe.config.ConfigFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RequestFactoryTest {
    public static final String BASE_URL = "http://example.com";
    private final DownloadContext context = mock(DownloadContext.class);
    private final RequestFactory factory = new RequestFactory(context);

    @Before
    public void setupContext() throws Exception {
        when(context.render(anyString(), anyListOf(Object.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
    }

    @Test
    public void basicJsonPost() throws Exception {
        when(context.getBaseUrl()).thenReturn(BASE_URL);
        Config config = config("application/json", "post", "/path", "value");

        HttpPost request = (HttpPost) factory.request(config, Collections.emptyList());

        verify(context).getBaseUrl();
        verify(context).render("/path", Collections.emptyList());
        verify(context).render("param", Collections.emptyList());
        verify(context).render("\"value\"", Collections.emptyList());
        verifyNoMoreInteractions(context);
        assertThat(request).isInstanceOf(HttpPost.class);
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getURI().toASCIIString()).isEqualTo(BASE_URL + "/path");
        assertThat(getEntity(request)).isEqualTo("{\"param\":\"value\"}");
    }

    @Test
    public void postsJsonData() throws Exception {
        Config config = parseString("{ contentType: application/json, method: post, path: /path, data: {} }");

        HttpPost request = (HttpPost) factory.request(config, Collections.emptyList());

        assertThat(getEntity(request)).isEqualTo("{}");
    }

    @Test
    public void addsHeadersFromConfig() throws Exception {
        Config config = config("application/json", "get", "/path", "value")
                .withFallback(parseString("{ headers: { Referer: \"http://referer.com\" } }"));

        HttpUriRequest request = factory.request(config, Collections.emptyList());

        assertThat(Streams.map(Stream.of(request.getHeaders("Referer")), Header::getValue)).contains("http://referer.com");
    }

    @Test
    public void postsFormData() throws Exception {
        Config config = config("application/x-www-form-urlencoded", "post", "/path", "value");

        HttpPost request = (HttpPost) factory.request(config, Collections.emptyList());

        assertThat(getEntity(request)).isEqualTo("param=value");
    }

    @Test
    public void unquotesConfigKeys() throws Exception {
        Config config = config("application/x-www-form-urlencoded", "post", "/path", "value")
                .withFallback(parseString("{ data { \"param:2\" = value2 } }"));

        HttpPost request = (HttpPost) factory.request(config, Collections.emptyList());

        assertThat(getEntity(request)).isEqualTo("param%3A2=value2&param=value");
    }

    private Config config(String contentType, String method, String path, String value) {
        return ConfigFactory.parseMap(ImmutableMap.of(
                "contentType", contentType,
                "method", method,
                "path", path,
                "data.param", value));
    }

    private String getEntity(HttpPost request) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        request.getEntity().writeTo(stream);
        return new String(stream.toByteArray());
    }
}