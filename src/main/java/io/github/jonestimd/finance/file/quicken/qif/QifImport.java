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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.github.jonestimd.commandline.CommandLine;
import io.github.jonestimd.finance.MessageKey;
import io.github.jonestimd.finance.config.ConfigManager;
import io.github.jonestimd.finance.dao.HibernateDaoContext;
import io.github.jonestimd.finance.file.FileImport;
import io.github.jonestimd.finance.file.quicken.QuickenContext;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.DriverService;
import io.github.jonestimd.finance.service.ServiceContext;
import org.apache.log4j.Logger;

public class QifImport implements FileImport { // TODO needs to be a service so the import is a single transaction
    private static final Logger logger = Logger.getLogger(QifImport.class);

    private Map<String, RecordConverter> converterMap;

    private AccountHolder accountHolder = new AccountHolder();
    private RecordConverter currentConverter;

    public QifImport(Collection<RecordConverter> converters) {
        this.converterMap = new HashMap<String, RecordConverter>();
        for (RecordConverter converter : converters) {
            for (String type : converter.getTypes()) {
                converterMap.put(type, converter);
            }
        }
    }

    private void debug(String pattern, Object ... args) { // TODO externalize messages
        if (logger.isDebugEnabled()) {
            logger.debug(MessageFormat.format(pattern, args));
        }
    }

    public void importFile(Reader reader) throws QuickenException, IOException {
        QifReader qifReader = new QifReader(reader);

        int importCount = 0;
        int ignoreCount = 0;
        try {
            for (QifRecord record = qifReader.nextRecord(); ! record.isEmpty(); record = qifReader.nextRecord()) {
                if (! record.isValid()) {
                    throw new QuickenException("invalidRecord", "QIF", record.getStartingLine());
                }
                if (record.isOption()) {
                    debug("ignoring option: !{0}", record.getControlValue());
                }
                else {
                    if (record.isControl()) {
                        currentConverter = converterMap.get(record.getControlValue());
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
            debug("imported {0} records, ignored {1} records", importCount, ignoreCount);
        }
        catch (QuickenException ex) {
            throw ex;
        }
        catch (Throwable ex) {
            throw new QuickenException("importFailed", ex, "QIF", qifReader.getLineNumber());
        }
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(args);
        if (commandLine.getInputCount() > 0) {
            try (FileReader qifReader = new FileReader(commandLine.getInput(0))) {
                boolean createSchema = commandLine.hasOption("--init-database");
                DriverService driver = new ConfigManager().loadDriver();
                HibernateDaoContext daoContext = HibernateDaoContext.connect(createSchema, driver, logger::info);
                FileImport qifImport = new QuickenContext(new ServiceContext(daoContext)).newQifImport();
                qifImport.importFile(qifReader);
            }
            catch (FileNotFoundException ex) {
                logger.error(ex.getMessage());
            }
            catch (QuickenException ex) {
                while (ex != null) {
                    logger.error(ex.getMessage());
                    if (ex.getCause() instanceof QuickenException) {
                        ex = (QuickenException) ex.getCause();
                    }
                    else {
                        if (ex.getCause() != null) {
                            logger.error(ex.getCause().getMessage(), ex.getCause());
                        }
                        break;
                    }
                }
            }
            catch (Throwable throwable) {
                logger.error(MessageKey.UNEXPECTED_EXCEPTION.key(), throwable);
            }
        }
        else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("usage: java " + QifImport.class.getName() + " qif_file");
    }
}