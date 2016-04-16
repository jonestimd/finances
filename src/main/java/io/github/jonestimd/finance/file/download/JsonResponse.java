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
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.spi.JsonProvider;

import com.typesafe.config.Config;
import io.github.jonestimd.util.Streams;
import org.apache.http.HttpEntity;

public class JsonResponse extends StepResponse<JsonValue> {
    private final JsonObject jsonObject;
    private JsonArray jsonTable;
    private List<String> tableColumns;

    public JsonResponse(HttpEntity entity) throws IOException {
        try (JsonReader reader = JsonProvider.provider().createReader(entity.getContent())) {
            jsonObject = reader.readObject();
        }
    }

    @Override
    protected String getValue(String selector) {
        return asString(jsonObject.get(selector));
    }

    @Override
    public List<List<Object>> getValues(Config config) {
        if (config.hasPath("format") && config.getString("format").equals("table")) {
            jsonTable = jsonObject.getJsonArray(config.getString("selector.path"));
            tableColumns = Streams.map(jsonTable.getJsonArray(0), this::asString);
        }
        return super.getValues(config);
    }

    @Override
    protected List<JsonValue> getRows(String selector) {
        if (jsonTable != null) {
            return jsonTable.subList(1, jsonTable.size());
        }
        return jsonObject.getJsonArray(selector);
    }

    @Override
    protected String getValue(JsonValue row) {
        return asString(row);
    }

    @Override
    protected String getValue(JsonValue row, String selector) {
        if (jsonTable != null) {
            return asString(((JsonArray) row).get(tableColumns.indexOf(selector)));
        }
        return asString(((JsonObject) row).get(selector));
    }

    private String asString(JsonValue value) {
        return value.getValueType().equals(ValueType.STRING) ? ((JsonString) value).getString() : value.toString();
    }
}
