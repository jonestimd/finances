// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.file.download;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import io.github.jonestimd.collection.MapBuilder;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

public class RequestFactory {
    private static final Map<String, ContentType> CONTENT_TYPE_MAP = Collections.unmodifiableMap(
            Stream.of(ContentType.APPLICATION_JSON, ContentType.APPLICATION_FORM_URLENCODED)
                    .collect(Collectors.toMap(ContentType::getMimeType, Function.identity())));
    private final Map<ContentType, BiFunction<Config, List<Object>, HttpEntity>> dataFormatMap = new MapBuilder<ContentType, BiFunction<Config, List<Object>, HttpEntity>>()
            .put(ContentType.APPLICATION_JSON, this::asJson)
            .put(ContentType.APPLICATION_FORM_URLENCODED, this::asFormData).get();
    private final Map<String, BiFunction<Config, List<Object>, HttpUriRequest>> factoryMap = new MapBuilder<String, BiFunction<Config, List<Object>, HttpUriRequest>>()
            .put("get", this::get)
            .put("post", this::post).get();

    private final Logger logger = Logger.getLogger(getClass());
    private final DownloadContext context;

    public RequestFactory(DownloadContext context) {
        this.context = context;
    }

    public HttpUriRequest request(Config config, List<Object> fileKeys) {
        String method = config.getString("method").toLowerCase();
        HttpUriRequest request = factoryMap.get(method).apply(config, fileKeys);
        if (config.hasPath("headers")) {
            for (Entry<String, ConfigValue> entry : config.getConfig("headers").entrySet()) {
                request.setHeader(unquote(entry.getKey()), context.render(entry.getValue().unwrapped().toString(), fileKeys));
            }
        }
        logger.info(request);
        return request;
    }

    private HttpGet get(Config config, List<Object> fileKeys) {
        String path = context.render(config.getString("path"), fileKeys);
        return new HttpGet(context.getBaseUrl() + path);
    }

    private HttpPost post(Config config, List<Object> fileKeys) {
        ContentType contentType = CONTENT_TYPE_MAP.get(config.getString("contentType"));
        HttpPost post = new HttpPost(context.getBaseUrl() + context.render(config.getString("path"), fileKeys));
        post.setEntity(dataFormatMap.get(contentType).apply(config.getConfig("data"), fileKeys));
        return post;
    }

    private StringEntity asJson(Config config, List<Object> fileKeys) {
        StringBuilder buffer = new StringBuilder("{");
        for (Entry<String, ConfigValue> entry : config.entrySet()) {
            buffer.append('"').append(context.render(unquote(entry.getKey()), fileKeys)).append("\":")
                    .append(context.render(entry.getValue().render(), fileKeys)).append(',');
        }
        if (buffer.charAt(buffer.length()-1) == ',') buffer.deleteCharAt(buffer.length()-1);
        return new StringEntity(buffer.append('}').toString(), ContentType.APPLICATION_JSON);
    }

    private UrlEncodedFormEntity asFormData(Config config, List<Object> fileKeys) {
        try {
            List<NameValuePair> formData = new ArrayList<>();
            for (Entry<String, ConfigValue> entry : config.entrySet()) {
                formData.add(new BasicNameValuePair(
                        context.render(unquote(entry.getKey()), fileKeys),
                        context.render(entry.getValue().unwrapped().toString(), fileKeys)));
            }
            return new UrlEncodedFormEntity(formData);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String unquote(String configKey) {
        return configKey.charAt(0) == '"' ? configKey.substring(1, configKey.length()-1) : configKey;
    }
}
