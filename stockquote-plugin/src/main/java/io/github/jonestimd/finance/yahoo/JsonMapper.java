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
package io.github.jonestimd.finance.yahoo;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import io.github.jonestimd.finance.yahoo.annotation.YahooColumn;
import io.github.jonestimd.finance.yahoo.annotation.YahooTable;
import io.github.jonestimd.util.Streams;

/**
 * Maps Yahoo query results to a Java class.
 * @param <T> the class representing an item in the query result and which is
 * annotated with {@link YahooTable} and {@link YahooColumn}
 */
public class JsonMapper<T> {
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    private final Constructor<T> constructor;
    private final String table;
    private final String resultName;
    private final Map<YahooColumn, Field> columnFields = new HashMap<>();
    private final Function<JsonValue, T> transform = input -> fromJson((JsonObject) input);

    public JsonMapper(Class<T> entityClass) throws NoSuchMethodException {
        YahooTable yahooTable = entityClass.getAnnotation(YahooTable.class);
        if (yahooTable == null) {
            throw new IllegalArgumentException("no @YahooTable on " + entityClass.getName());
        }
        this.constructor = entityClass.getDeclaredConstructor();
        this.constructor.setAccessible(true);
        this.table = yahooTable.name();
        this.resultName = yahooTable.result();
        for (Field field : entityClass.getDeclaredFields()) {
            YahooColumn yahooColumn = field.getAnnotation(YahooColumn.class);
            if (yahooColumn != null) {
                columnFields.put(yahooColumn, field);
                field.setAccessible(true);
            }
        }
        if (columnFields.isEmpty()) {
            throw new IllegalArgumentException("no fields with @YahooColumn found in " + entityClass.getName());
        }
    }

    public String getTable() {
        return table;
    }

    public String getResultName() {
        return resultName;
    }

    public Iterable<String> getColumns() {
        return Streams.map(columnFields.keySet(), YahooColumn::name);
    }

    public List<T> fromResult(InputStream stream) {
        JsonValue result = JSON_PROVIDER.createReader(stream).readObject()
                .getJsonObject("query")
                .getJsonObject("results")
                .get(resultName);
        if (result instanceof JsonArray) {
            return Streams.map((JsonArray) result, transform);
        }
        return Collections.singletonList(transform.apply(result));
    }

    private T fromJson(JsonObject jsonObject) {
        try {
            T result = constructor.newInstance();
            for (Entry<YahooColumn, Field> entry : columnFields.entrySet()) {
                entry.getValue().set(result, getValue(entry.getKey(), jsonObject));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("error parsing Yahoo query result", e);
        }
    }

    private Object getValue(YahooColumn yahooColumn, JsonObject jsonObject) {
        return yahooColumn.jsonType().get(jsonObject, yahooColumn.name());
    }
}
