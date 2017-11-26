// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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
package io.github.jonestimd.finance.stockquote.quandl;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import com.typesafe.config.Config;

public class JsonMapper {
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    private final List<String> symbolPath;
    private final List<String> columnsPath;
    private final List<String> tablePath;
    private final String priceColumn;

    public JsonMapper(Config config) {
        this.symbolPath = config.getStringList("symbolPath");
        this.columnsPath = config.getStringList("columnsPath");
        this.tablePath = config.getStringList("tablePath");
        this.priceColumn = config.getString("priceColumn");
    }

    public Map<String, BigDecimal> getPrice(InputStream stream) {
        JsonObject response = JSON_PROVIDER.createReader(stream).readObject();
        String symbol = getString(symbolPath, response);
        JsonArray columnNames = getArray(columnsPath, response);
        JsonArray row = getArray(tablePath, response).getJsonArray(0);
        for (int i = 0; i < columnNames.size(); i++) {
            if (priceColumn.equals(columnNames.getString(i))) {
                return Collections.singletonMap(symbol, row.getJsonNumber(i).bigDecimalValue());
            }
        }
        throw new IllegalArgumentException("Price not found in " + response.toString());
    }

    private JsonArray getArray(List<String> path, JsonObject obj) {
        JsonValue value = obj;
        for (String name : path) {
            value = ((JsonObject) value).get(name);
        }
        return (JsonArray) value;
    }

    private String getString(List<String> path, JsonObject obj) {
        JsonValue value = obj;
        for (String name : path) {
            value = ((JsonObject) value).get(name);
        }
        return ((JsonString) value).getString();
    }
}
