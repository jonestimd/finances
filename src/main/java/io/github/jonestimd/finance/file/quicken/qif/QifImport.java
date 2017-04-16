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
package io.github.jonestimd.finance.file.quicken.qif;

import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.github.jonestimd.finance.file.FileImport;
import io.github.jonestimd.finance.file.ImportSummary;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.function.MessageConsumer;
import org.apache.log4j.Logger;

public class QifImport implements FileImport {
    private Logger logger = Logger.getLogger(QifImport.class);
    private Map<String, RecordConverter> converterMap;

    private AccountHolder accountHolder = new AccountHolder();
    private RecordConverter currentConverter;

    public QifImport(Collection<RecordConverter> converters) {
        this.converterMap = new HashMap<>();
        for (RecordConverter converter : converters) {
            for (String type : converter.getTypes()) {
                converterMap.put(type, converter);
            }
        }
    }

    public ImportSummary importFile(Reader reader, MessageConsumer updateProgress) throws QuickenException {
        QifReader qifReader = new QifReader(reader);
        accountHolder.onAcountChange(account -> updateProgress.accept("import.qif.switch.account.status", account.getName()));

        int importCount = 0;
        int ignoreCount = 0;
        try {
            for (QifRecord record = qifReader.nextRecord(); ! record.isEmpty(); record = qifReader.nextRecord()) {
                if (! record.isValid()) {
                    throw new QuickenException("invalidRecord", "QIF", record.getStartingLine());
                }
                if (record.isOption()) {
                    updateProgress.accept("import.qif.ignoredOption", record.getControlValue());
                }
                else {
                    if (record.isControl()) {
                        currentConverter = converterMap.get(record.getControlValue());
                        if (currentConverter == null) updateProgress.accept("import.qif.ignoredControl", record.getControlValue());
                        else updateProgress.accept(currentConverter.getStatusKey(), accountHolder.getAccountName());
                    }
                    else if (currentConverter != null) {
                        currentConverter.importRecord(accountHolder, record);
                        importCount++;
                    }
                    else {
                        ignoreCount++;
                    }
                }
            }
            return new ImportSummary(importCount, ignoreCount);
        } catch (QuickenException ex) {
            logger.error("Quicken import failed", ex);
            throw ex;
        } catch (Throwable ex) {
            logger.error("Quicken import failed", ex);
            throw new QuickenException("importFailed", ex, "QIF", qifReader.getLineNumber());
        }
    }
}