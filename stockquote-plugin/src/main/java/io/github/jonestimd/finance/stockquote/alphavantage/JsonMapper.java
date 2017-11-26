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
package io.github.jonestimd.finance.stockquote.alphavantage;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import com.typesafe.config.Config;

public class JsonMapper {
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    private final List<String> symbolPath;
    private final List<String> seriesPath;
    private final String priceKey;
    private final String errorKey;

    public JsonMapper(Config config) {
        this.symbolPath = config.getStringList("symbolPath");
        this.seriesPath = config.getStringList("seriesPath");
        this.priceKey = config.getString("priceKey");
        this.errorKey = config.getString("errorKey");
    }

    public Map<String, BigDecimal> getPrice(InputStream stream) {
        JsonObject response = JSON_PROVIDER.createReader(stream).readObject();
        String error = response.getString(errorKey, null);
        if (error != null) throw new IllegalStateException(error);
        String symbol = getString(symbolPath, response);
        JsonObject series = getObject(seriesPath, response);
        return series.keySet().stream().max(String::compareTo)
                .map(date -> getString(priceKey, series.getJsonObject(date)))
                .map(price -> Collections.singletonMap(symbol, new BigDecimal(price)))
                .orElse(Collections.emptyMap());
    }

    private JsonObject getObject(List<String> path, JsonObject obj) {
        JsonObject value = obj;
        for (String keyRegex : path) {
            value = (JsonObject) getValue(keyRegex, value);
        }
        return value;
    }

    private String getString(List<String> path, JsonObject obj) {
        return ((JsonString) getValue(path, obj)).getString();
    }

    private String getString(String keyRegex, JsonObject object) {
        return ((JsonString) getValue(keyRegex, object)).getString();
    }

    private JsonValue getValue(List<String> path, JsonObject object) {
        JsonValue value = object;
        for (String keyRegex : path) {
            value = getValue(keyRegex, (JsonObject) value);
        }
        return value;
    }

    private JsonValue getValue(String keyRegex, JsonObject object) {
        for (Entry<String, JsonValue> entry : object.entrySet()) {
            if (entry.getKey().matches(keyRegex)) return entry.getValue();
        }
        throw new IllegalArgumentException("No key matches " + keyRegex + " in " + object.toString());
    }
}
