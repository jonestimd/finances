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

public class ImportSummary {
    private final int imported;
    private final int ignored;
    private final int totalRecords;

    public ImportSummary(int imported, int ignored) {
        this(imported, ignored, imported + ignored);
    }

    public ImportSummary(int imported, int ignored, int totalRecords) {
        this.imported = imported;
        this.ignored = ignored;
        this.totalRecords = totalRecords;
    }

    public int getImported() {
        return imported;
    }

    public int getIgnored() {
        return ignored;
    }

    public int getTotalRecords() {
        return totalRecords;
    }
}
