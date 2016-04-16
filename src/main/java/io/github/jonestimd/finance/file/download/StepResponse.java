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

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import io.github.jonestimd.util.Streams;

/**
 * Process the response for an HTTP request.
 * <p>
 * Configuration options:
 * <pre>
 * <strong><em>extract</em></strong> [{ # save values for use in following steps
 *   <strong>name</strong> = <i>variable-name</i>
 *   <strong>selector</strong> = <i>field-name</i>
 *   <strong>type</strong> = <i>{string|number|date}</i>
 *   <strong>format</strong> = <i>value-format</i>
 * }]
 *
 * or
 *
 * <strong><em>files</em></strong> { # save file info for use in download steps
 *   <strong>format</strong> = <em>sub-format</em> # e.g. table for json
 *   <strong>selector</strong> {
 *     <strong>path</strong> = <em>field-name</em>
 *     <strong>fields</strong> = [ # simple selector or typed selector
 *       {<strong>selector</strong> = <em>field-name</em>, <strong>type</strong> = <em>{string|number|date}</em>, <strong>format</strong> = <em>value-format</em>},
 *       <em>field-name</em>
 *     ]
 *   }
 * }
 * </pre>
 * Where {@code selector} depends on the result type.
 */
public abstract class StepResponse<T> {
    public static final String FIELDS_CONFIG_KEY = "selector.fields";
    public static final String PATH_CONFIG_KEY = "selector.path";
    public static final List<String> VALID_TYPES = ImmutableList.of("string", "date", "number");

    public Object getValue(Config config) throws ParseException {
        FieldConfig fieldConfig = new FieldConfig(config);
        return fieldConfig.parseValue(getValue(fieldConfig.selector));
    }

    protected abstract String getValue(String selector);

    public List<List<Object>> getValues(Config config) {
        List<FieldConfig> fieldSelectors = config.hasPath(FIELDS_CONFIG_KEY) ? Streams.map(config.getList(FIELDS_CONFIG_KEY), this::fieldConfig) : Collections.emptyList();
        List<List<Object>> values = new ArrayList<>();
        if (fieldSelectors.isEmpty()) {
            for (T row : getRows(config.getString(PATH_CONFIG_KEY))) {
                values.add(Collections.singletonList(getValue(row)));
            }
        }
        else {
            for (T row : getRows(config.getString(PATH_CONFIG_KEY))) {
                values.add(Streams.map(fieldSelectors, selector -> selector.getValue(row)));
            }
        }
        return values;
    }

    protected abstract List<T> getRows(String selector);
    protected abstract String getValue(T row);
    protected abstract String getValue(T row, String selector);

    private FieldConfig fieldConfig(ConfigValue config) {
        if (config instanceof ConfigObject) {
            return new FieldConfig(((ConfigObject) config).toConfig());
        }
        return new FieldConfig(config.unwrapped().toString());
    }

    private class FieldConfig {
        private final String selector;
        private final String type;
        private final String format;

        private FieldConfig(String selector) {
            this.selector = selector;
            type = null;
            format = null;
        }

        private FieldConfig(Config config) {
            selector = config.getString("selector");
            if (config.hasPath("type")) {
                type = config.getString("type");
                if (! VALID_TYPES.contains(type)) {
                    throw new IllegalArgumentException("Invalid type: " + type);
                }
                format = type.equals("string") ? null : config.getString("format");
            }
            else {
                format = null;
                type = null;
            }
        }

        public Object getValue(T row) {
            return parseValue(StepResponse.this.getValue(row, selector));
        }

        public Object parseValue(String value) {
            try {
                if (format != null) {
                    if ("date".equals(type)) {
                        return new SimpleDateFormat(format).parse(value);
                    }
                    if ("number".equals(type)) {
                        return new DecimalFormat(format).parse(value);
                    }
                }
                return value;
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
