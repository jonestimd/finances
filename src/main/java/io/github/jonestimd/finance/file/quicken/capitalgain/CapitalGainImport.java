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
package io.github.jonestimd.finance.file.quicken.capitalgain;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.github.jonestimd.commandline.CommandLine;
import io.github.jonestimd.finance.MessageKey;
import io.github.jonestimd.finance.config.ConfigManager;
import io.github.jonestimd.finance.dao.HibernateDaoContext;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.file.FileImport;
import io.github.jonestimd.finance.file.quicken.QuickenContext;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.service.ServiceContext;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.SwingContext;
import io.github.jonestimd.finance.swing.securitylot.LotAllocationDialog;
import io.github.jonestimd.function.MessageConsumer;
import io.github.jonestimd.util.MessageHelper;
import org.apache.log4j.Level;

public class CapitalGainImport implements FileImport {
    private static final MessageHelper messageHelper = new MessageHelper(BundleType.MESSAGES.get(), CapitalGainImport.class);
    private static final Predicate<SecurityLot> IS_LOT_NOT_EMPTY = input -> BigDecimal.ZERO.compareTo(input.getPurchaseShares()) != 0;
    private final TransactionService transactionService;
    private final Map<String, Map<Date, List<CapitalGain>>> saleMap = new HashMap<>();
    private final LotValidator lotValidator;
    private int recordCount = 0;
    private int ignoreCount = 0;

    public CapitalGainImport(TransactionService transactionService, LotAllocationDialog lotAllocationDialog) {
        this.transactionService = transactionService;
        this.lotValidator = new LotValidator(lotAllocationDialog);
    }

    public void importFile(Reader reader, MessageConsumer updateProgress) throws QuickenException {
        CapitalGainReader tsvReader = new CapitalGainReader(reader);

        try {
            tsvReader.readHeader();
            for (CapitalGainRecord record = tsvReader.nextRecord(); record != null; record = tsvReader.nextRecord()) {
                putInDateMap(record.getSecurityName(), record.getSellDate(), saleMap, record);
                recordCount++;
            }
            Map<CapitalGain, SecurityLot> recordLotMap = new SaleMatcher(transactionService).assignSales(saleMap);
            Set<SecurityLot> saleLots = new PurchaseMatcher(transactionService).assignPurchases(recordLotMap);
            lotValidator.validateLots(saleLots);
            if (! saleLots.isEmpty()) {
                transactionService.saveSecurityLots(Iterables.filter(saleLots, IS_LOT_NOT_EMPTY));
            }
            updateProgress.accept("importSummary", recordLotMap.size(), ignoreCount, recordCount+ignoreCount);
        }
        catch (IOException ex) {
            throw new QuickenException("importFailed", ex, "TXF", tsvReader.getLineNumber());
        }
    }

    private <K> void putInDateMap(K key, Date date, Map<K, Map<Date, List<CapitalGain>>> parentMap, CapitalGain record) throws QuickenException {
        Map<Date, List<CapitalGain>> dateMap = parentMap.computeIfAbsent(key, k -> new HashMap<>());
        List<CapitalGain> records = dateMap.computeIfAbsent(date, k -> new ArrayList<>());
        records.add(record);
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(args);
        if (commandLine.getInputCount() > 0) {
            try {
                UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
            }
            catch (Exception e) {
                messageHelper.warn("failed to set look and feel");
            }
            try (FileReader txfReader = new FileReader(commandLine.getInput(0))) {
                messageHelper.getLogger().getParent().setLevel(Level.DEBUG);
                ServiceContext serviceContext = new ServiceContext(new HibernateDaoContext(new ConfigManager().loadDriver()));
                SwingContext swingContext = new SwingContext(serviceContext);
                FileImport txfImport = new QuickenContext(serviceContext)
                        .newTxfImport(new LotAllocationDialog(JOptionPane.getRootFrame(), swingContext.getTableFactory()));
                txfImport.importFile(txfReader, messageHelper);
            }
            catch (FileNotFoundException ex) {
                messageHelper.error(ex.getMessage(), null);
            }
            catch (QuickenException ex) {
                while (ex != null) {
                    messageHelper.error(ex.getMessage());
                    if (ex.getCause() instanceof QuickenException) {
                        ex = (QuickenException) ex.getCause();
                    }
                    else {
                        if (ex.getCause() != null) {
                            messageHelper.error(ex.getCause().getMessage(), ex.getCause());
                        }
                        break;
                    }
                }
            }
            catch (Throwable throwable) {
                messageHelper.error(MessageKey.UNEXPECTED_EXCEPTION.key(), throwable);
            }
        }
        else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("usage: java " + CapitalGainImport.class.getName() + " scd_file txf_file");
    }
}