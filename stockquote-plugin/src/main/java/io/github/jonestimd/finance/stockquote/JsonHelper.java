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
package io.github.jonestimd.finance.stockquote;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

public class JsonHelper {
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    private final JsonObject root;

    public JsonHelper(InputStream stream) {
        root = JSON_PROVIDER.createReader(stream).readObject();
    }

    public String getString(List<String> path) {
        return asString(getValue(path, root));
    }

    public JsonArray getArray(List<String> path) {
        return (JsonArray) getValue(path, root);
    }

    private static JsonValue getValue(List<String> path, JsonObject root) {
        JsonValue value = root;
        for (String key : path) {
            value = getValue(key, (JsonObject) value);
        }
        return value;
    }

    public static JsonValue getValue(String key, JsonObject object) {
        return optionalValue(key, object).orElseThrow(() -> new IllegalArgumentException("No value for " + key + " in " + object.toString()));
    }

    public Optional<String> optionalString(String key) {
        return optionalValue(key, root).map(JsonHelper::asString);
    }

    public static Optional<JsonValue> optionalValue(String key, JsonObject object) {
        return object.entrySet().stream().filter(entry -> key.equals(entry.getKey())).findFirst().map(Entry::getValue);
    }

    public JsonObject findObject(List<String> path) {
        return (JsonObject) findValue(path);
    }

    public String findString(List<String> path) {
        return asString(findValue(path));
    }

    public JsonValue findValue(List<String> path) {
        JsonValue value = root;
        for (String keyRegex : path) {
            value = findValue(keyRegex, (JsonObject) value);
        }
        return value;
    }

    public static String findString(String keyRegex, JsonObject object) {
        return asString(findValue(keyRegex, object));
    }

    private static JsonValue findValue(String keyRegex, JsonObject object) {
        return object.entrySet().stream().filter(entry -> entry.getKey().matches(keyRegex)).findFirst().map(Entry::getValue)
                .orElseThrow(() -> new IllegalArgumentException("No key matches " + keyRegex + " in " + object.toString()));
    }

    public <T> Map<String, T> mapEntries(List<String> subPath, Function<JsonValue, T> converter) {
        Map<String, T> values = new HashMap<>();
        for (Entry<String, JsonValue> entry : root.entrySet()) {
            values.put(entry.getKey(), converter.apply(getValue(subPath, (JsonObject) entry.getValue())));
        }
        return values;
    }

    public static String asString(JsonValue value) {
        return ((JsonString) value).getString();
    }

    public static BigDecimal asBigDecimal(JsonValue value) {
        return ((JsonNumber) value).bigDecimalValue();
    }
}
