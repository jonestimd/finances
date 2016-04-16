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
package io.github.jonestimd.finance.file.quicken.capitalgain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CapitalGainReader {
    private final BufferedReader reader;
    private final List<String> columns = new ArrayList<String>();
    private long lineNumber = 0L;

    public CapitalGainReader(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    public void readHeader() throws IOException {
        String line = nextLine();
        if (line == null) {
            throw new IOException("Unexpected end of file at line " + lineNumber);
        }
        columns.addAll(Arrays.asList(line.split("\t")));
    }

    public CapitalGainRecord nextRecord() throws IOException {
        String line = nextLine();
        if (line != null) {
            Map<String, String> values = new HashMap<String, String>();
            int i = 0;
            for (String value : line.split("\t")) {
                values.put(columns.get(i++), value);
            }
            return new CapitalGainRecord(lineNumber, values);
        }
        return null;
    }

    private String nextLine() throws IOException {
        String line;
        do {
            line = reader.readLine();
            lineNumber++;
        } while (line != null && line.charAt(0) == '\t');
        return line;
    }

    public long getLineNumber() {
        return lineNumber;
    }
}