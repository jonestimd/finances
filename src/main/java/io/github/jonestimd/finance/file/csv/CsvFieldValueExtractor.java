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

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.file.FieldValueExtractor;
import io.github.jonestimd.util.Streams;

public class CsvFieldValueExtractor implements FieldValueExtractor {
    private final Collection<ImportField> importFields;

    public CsvFieldValueExtractor(Map<String, ImportField> importFields) {
        this.importFields = importFields.values();
    }

    @Override
    public Iterable<Multimap<ImportField, String>> parse(InputStream inputStream) throws Exception {
        return Streams.map(new CsvParser(inputStream).getStream(), this::mapRecord);
    }

    protected Multimap<ImportField, String> mapRecord(Map<String, String> record) {
        return Multimaps.forMap(importFields.stream()
                .filter(field -> record.containsKey(field.getLabel()))
                .collect(Collectors.toMap(Function.identity(), field -> record.get(field.getLabel()))));
    }
}
