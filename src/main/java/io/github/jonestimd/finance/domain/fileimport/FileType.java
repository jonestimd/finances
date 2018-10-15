// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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
package io.github.jonestimd.finance.domain.fileimport;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.collect.ListMultimap;
import io.github.jonestimd.finance.file.ImportFieldMapper;
import io.github.jonestimd.finance.file.csv.CsvParser;
import io.github.jonestimd.finance.file.excel.SheetParser;
import io.github.jonestimd.finance.file.pdf.PdfFieldValueExtractor;
import io.github.jonestimd.finance.swing.BundleType;

public enum  FileType {
    CSV("csv", (importFile, stream) -> new ImportFieldMapper(importFile.getFields()).mapFields(new CsvParser(stream).getStream())),
    XLS("xls", (importFile, stream) -> new ImportFieldMapper(importFile.getFields()).mapFields(new SheetParser(stream, 0, importFile.getStartOffset()).getStream())),
    PDF("pdf", (importFile, stream) -> new PdfFieldValueExtractor(importFile.getFields()).parse(stream));

    public final String extension;
    private final Parser parser;

    FileType(String extension, Parser parser) {
        this.extension = extension;
        this.parser = parser;
    }

    public Iterable<ListMultimap<ImportField, String>> parse(ImportFile importFile, InputStream stream) throws IOException {
        return parser.apply(importFile, stream);
    }

    @Override
    public String toString() {
        return BundleType.REFERENCE.getString("importFileType." + name());
    }

    private interface Parser {
        Iterable<ListMultimap<ImportField, String>> apply(ImportFile importFile, InputStream stream) throws IOException;
    }
}
