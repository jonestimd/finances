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
package io.github.jonestimd.finance.file;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.domain.fileimport.FieldType.*;

public abstract class ImportContext {
    private static final Map<FieldType, TransactionValueConsumer> TRANSACTION_CONSUMERS = ImmutableMap.of(
            DATE, ImportContext::setDate,
            PAYEE, ImportContext::setPayee
    );
    protected final ImportFile importFile;
    protected final DomainMapper<Payee> payeeMapper;
    protected final DomainMapper<TransactionCategory> categoryMapper;

    protected ImportContext(ImportFile importFile, DomainMapper<Payee> payeeMapper, DomainMapper<TransactionCategory> categoryMapper) {
        this.importFile = importFile;
        this.payeeMapper = payeeMapper;
        this.categoryMapper = categoryMapper;
    }

    public List<Transaction> parseTransactions(InputStream source) throws Exception {
        Iterable<Map<ImportField, String>> records = importFile.getFieldValueExtractor().parse(source);
        return Streams.of(records).map(this::toTransaction).collect(Collectors.toList());
    }

    private Transaction toTransaction(Map<ImportField, String> fieldValues) {
        Transaction transaction = new Transaction(importFile.getAccount(), new Date(), importFile.getPayee(), false, null); // TODO override account in UI?
        fieldValues.entrySet().stream()
                .filter(entry -> !updateTransaction(transaction, entry.getValue(), entry.getKey()))
                .forEach(entry -> updateDetails(transaction, entry.getValue(), entry.getKey()));
        return transaction;
    }

    private boolean updateTransaction(Transaction transaction, String value, ImportField field) {
        TransactionValueConsumer consumer = TRANSACTION_CONSUMERS.get(field.getType());
        if (consumer != null) {
            consumer.accept(this, transaction, value, field);
            return true;
        }
        return false;
    }

    protected abstract void updateDetails(Transaction transaction, String value, ImportField field);

    private void setDate(Transaction transaction, String value, ImportField field) {
        transaction.setDate(field.parseDate(value));
    }

    private void setPayee(Transaction transaction, String value, ImportField field) {
        transaction.setPayee(payeeMapper.get(value));
    }

    private interface TransactionValueConsumer {
        void accept(ImportContext context, Transaction transaction, String value, ImportField field);
    }
}
