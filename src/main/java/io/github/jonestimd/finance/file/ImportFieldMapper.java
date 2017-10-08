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
package io.github.jonestimd.finance.file;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.util.Streams;

import static com.google.common.collect.Multimaps.*;

public class ImportFieldMapper {
    private static final Supplier<ListMultimap<ImportField, String>> MULTIMAP_SUPPLIER = MultimapBuilder.hashKeys().arrayListValues()::build;
    private final Collection<ImportField> importFields;

    public ImportFieldMapper(Collection<ImportField> importFields) {
        this.importFields = importFields;
    }

    public Iterable<ListMultimap<ImportField, String>> mapFields(Stream<Map<String, String>> rows) {
        return Streams.map(rows, this::mapRecord);
    }

    private ListMultimap<ImportField, String> mapRecord(Map<String, String> record) {
        return importFields.stream().map(field -> new FieldValue(field, field.getValue(record)))
                .filter(pair -> pair.value != null)
                .collect(toMultimap(pair -> pair.field, pair -> pair.value, MULTIMAP_SUPPLIER));
    }

    private static class FieldValue {
        private final ImportField field;
        private final String value;

        private FieldValue(ImportField field, String value) {
            this.field = field;
            this.value = value;
        }
    }
}
