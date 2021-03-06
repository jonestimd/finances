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
package io.github.jonestimd.finance.file.quicken.qif;

import java.io.Reader;

import io.github.jonestimd.finance.file.quicken.QuickenReader;
import io.github.jonestimd.finance.swing.BundleType;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class QifReader extends QuickenReader<QifRecord> {
    private Logger logger = Logger.getLogger(QifReader.class);

    public QifReader(Reader reader) {
        super(reader);
    }

    protected QifRecord createRecord(long currentLine) {
        return new QifRecord(currentLine);
    }

    protected void validateLine(String line) {
        if (QifField.fromCode(line.charAt(0)) == null && logger.isEnabledFor(Level.WARN)) {
            logger.warn(BundleType.MESSAGES.formatMessage("import.qif.unknownQifFieldCode", line));
        }
    }
}