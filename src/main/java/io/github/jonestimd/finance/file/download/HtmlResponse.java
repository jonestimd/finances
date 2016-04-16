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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * <h4>Selectors</h4>
 * Value selectors specified in the configuration consist of a CSS selector followed by one of the following.
 * <ul>
 *     <li>{@code /@attr} to use the value of the specified attribute on the selected element</li>
 *     <li>{@code /method()} to use the value returned by the specified method of {@link Element}</li>
 * </ul>
 * If neither is specified then the default is {@code /text()}.
 */
public class HtmlResponse extends StepResponse<Element> {
    private static final Pattern SELECTOR_PATTERN = Pattern.compile("^(.*?)(?:/(@\\w+|\\w+\\(\\)))?$");
    private final Document document;

    public HtmlResponse(HttpEntity entity, String baseUrl) throws IOException {
        document = Jsoup.parse(entity.getContent(), "UTF-8", baseUrl);
    }

    @Override
    protected String getValue(String selector) {
        Matcher matcher = SELECTOR_PATTERN.matcher(selector);
        matcher.find();
        Elements elements = document.select(matcher.group(1));
        if (elements.isEmpty()) {
            return null;
        }
        Element element = elements.get(0);
        if (matcher.group(2) != null) {
            return getValue(element, matcher.group(2));
        }
        return element.text();
    }

    @Override
    protected List<Element> getRows(String selector) {
        return document.select(selector);
    }

    @Override
    protected String getValue(Element row) {
        return row.text();
    }

    @Override
    protected String getValue(Element element, String selector) {
        if (selector.charAt(0) == '@') {
            return element.attr(selector.substring(1));
        }
        if (selector.endsWith("()")) {
            try {
                Method method = Element.class.getMethod(selector.substring(0, selector.length() - 2));
                if (method.getReturnType().equals(String.class)) {
                    return (String) method.invoke(element);
                }
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Unknown selector: " + selector);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getTargetException());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("Unknown selector: " + selector);
    }
}
