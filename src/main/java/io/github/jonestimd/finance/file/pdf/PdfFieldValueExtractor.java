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
package io.github.jonestimd.finance.file.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.itextpdf.text.pdf.parser.Vector;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.file.FieldValueExtractor;
import io.github.jonestimd.util.Streams;

public class PdfFieldValueExtractor implements FieldValueExtractor {
    private final Collection<ImportField> importFields;

    public PdfFieldValueExtractor(Map<String, ImportField> importFields) {
        this.importFields = importFields.values();
    }

    @Override
    public Iterable<Map<ImportField, String>> parse(InputStream inputStream) throws IOException {
        return Collections.singleton(getFieldValues(new TextExtractor(inputStream).getText()));
    }

    protected Map<ImportField, String> getFieldValues(Stream<Entry<Vector, String>> pdfText) throws IOException {
        return Maps.filterValues(Maps.transformValues(new TextWalker(pdfText::iterator).fieldValues, StringBuilder::toString), Predicates.not(String::isEmpty));
    }

    private class TextWalker {
        private final Map<ImportField, StringBuilder> fieldValues = new LinkedHashMap<>();
        private float x = -1f;
        private float y = -1f;
        private final StringBuilder prefix = new StringBuilder();

        public TextWalker(Iterable<Entry<Vector, String>> pdfText) throws IOException {
            for (Entry<Vector, String> entry : pdfText) {
                x = entry.getKey().get(Vector.I1);
                if (entry.getKey().get(Vector.I2) != y || isPastRightEdge(x)) {
                    prefix.setLength(0);
                    y = entry.getKey().get(Vector.I2);
                }
                List<ImportField> fieldCandidates = getMatches();
                if (fieldCandidates.size() == 1) {
                    ImportField field = fieldCandidates.get(0);
                    if (field.getLabel().equals(prefix.toString().trim()) && field.isValueRegion(x)) {
                        appendValue(field, entry.getValue());
                    }
                    else if (field.isLabelRegion(x)) {
                        appendPrefix(entry.getValue());
                    }
                }
                else if (! fieldCandidates.isEmpty()) {
                    appendPrefix(entry.getValue());
                }
            }
        }

        private List<ImportField> getMatches() {
            return Streams.filter(importFields, this::isMatch);
        }

        private boolean isMatch(ImportField field) {
            return field.getLabel().startsWith(prefix.toString().trim()) && field.isInRegion(x, y);
        }

        protected void appendPrefix(String value) {
            prefix.append(value).append(' ');
            List<ImportField> fieldCandidates = getMatches();
            if (fieldCandidates.size() == 1) {
                ImportField field = fieldCandidates.get(0);
                if (field.getLabel().equals(prefix.toString().trim())) {
                    fieldValues.put(field, new StringBuilder());
                }
            }
        }

        private boolean isPastRightEdge(float x) {
            return importFields.stream().filter(this::isMatch).allMatch(field -> field.isPastRightEdge(x));
        }

        protected void appendValue(ImportField field, String value) {
            StringBuilder buffer = fieldValues.get(field);
            if (buffer.length() > 0) {
                buffer.append(' ');
            }
            buffer.append(value);
        }
    }
}
