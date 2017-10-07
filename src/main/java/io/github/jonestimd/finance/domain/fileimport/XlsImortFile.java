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
package io.github.jonestimd.finance.domain.fileimport;

import java.io.IOException;
import java.io.InputStream;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.google.common.collect.Multimap;
import io.github.jonestimd.finance.file.ImportFieldMapper;
import io.github.jonestimd.finance.file.excel.SheetParser;

@Entity
@DiscriminatorValue("XLS")
public class XlsImortFile extends ImportFile {
    public XlsImortFile() {}

    @Override
    public Iterable<Multimap<ImportField, String>> parse(InputStream stream) throws IOException {
        return new ImportFieldMapper(getFields()).mapFields(new SheetParser(stream, 0, getStartOffset()).getStream());
    }

    @Override
    public String getFileExtension() {
        return "xls";
    }
}
