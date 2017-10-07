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
package io.github.jonestimd.finance.file.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.github.jonestimd.finance.file.FileParser;

import static io.github.jonestimd.util.JavaPredicates.*;

public class CsvParser implements FileParser {
    private static final Pattern COLUMN_PATTERN = Pattern.compile("(?<value>[^,\"]*|\"[^\"]*\")(,|$)");
    private final Stream<Map<String, String>> stream;
    private final List<String> headers;

    public CsvParser(InputStream inputStream) throws IOException {
        this(new BufferedReader(new InputStreamReader(inputStream)));
    }

    public CsvParser(BufferedReader reader) throws IOException {
        headers = reader.lines().map(String::trim).findFirst().map(header -> Arrays.asList(header.split(","))).orElse(Collections.emptyList());
        stream = reader.lines().map(String::trim).filter(not(String::isEmpty)).map(this::parseLine);
    }

    public Stream<Map<String, String>> getStream() {
        return stream;
    }

    private Map<String, String> parseLine(String line) {
        Map<String, String> record = new HashMap<>();
        Matcher matcher = COLUMN_PATTERN.matcher(line);
        headers.stream().forEach(header -> record.put(header, matcher.find() ? unquote(matcher.group("value")) : ""));
        return record;
    }

    private String unquote(String value) {
        if (value.matches("^\".*\"$")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }
}
