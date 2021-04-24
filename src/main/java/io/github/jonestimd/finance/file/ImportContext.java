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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;

public abstract class ImportContext {
    protected final ImportFile importFile;
    protected final DomainMapper<Payee> payeeMapper;
    protected final DomainMapper<Security> securityMapper;

    protected ImportContext(ImportFile importFile, DomainMapper<Payee> payeeMapper, DomainMapper<Security> securityMapper) {
        this.importFile = importFile;
        this.payeeMapper = payeeMapper;
        this.securityMapper = securityMapper;
    }

    public List<Transaction> parseTransactions(InputStream source) throws Exception {
        List<Transaction> transactions = new ArrayList<>();
        Iterable<ListMultimap<ImportField, String>> records = importFile.parse(source);
        for (ListMultimap<ImportField, String> record : records) {
            updateDetails(getTransaction(record, transactions), record);
        }
        return transactions;
    }

    protected Transaction getTransaction(ListMultimap<ImportField, String> record, List<Transaction> transactions) {
        Transaction transaction = new Transaction(importFile.getAccount(), new Date(), importFile.getPayee(), false, null);
        Multimaps.asMap(record).forEach((key, value) -> updateTransaction(transaction, key, value));
        transactions.add(transaction);
        return transaction;
    }

    private void updateTransaction(Transaction transaction, ImportField field, List<String> values) {
        switch (field.getType()) {
            case DATE:
                setDate(transaction, values); break;
            case PAYEE:
                setPayee(transaction, values); break;
            case SECURITY:
                setSecurity(transaction, values); break;
        }
    }

    private void updateDetails(Transaction transaction, ListMultimap<ImportField, String> record) {
        Multimaps.asMap(record).entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().getType())).forEachOrdered(entry -> {
            if (!entry.getKey().getType().isTransaction()) {
                updateDetail(getDetail(transaction, record), entry.getKey(), entry.getValue());
            }
        });
        transaction.getDetails().removeIf(TransactionDetail::isZeroAmount);
    }

    protected abstract TransactionDetail getDetail(Transaction transaction, Multimap<ImportField, String> record);

    private void updateDetail(TransactionDetail detail, ImportField field, List<String> values) {
        DetailValueConsumer consumer = getDetailConsumer(field);
        Preconditions.checkNotNull(consumer, "Invalid field type: %s", field.getType());
        consumer.accept(detail, field, values);
    }

    protected DetailValueConsumer getDetailConsumer(ImportField field) {
        switch (field.getType()) {
            case CATEGORY:
                return this::setCategory;
            case TRANSFER_ACCOUNT:
                return this::setTransferAccount;
            case ASSET_QUANTITY:
                return this::setAssetQuantity;
        }
        return null;
    }

    private void setDate(Transaction transaction, List<String> values) {
        transaction.setDate(importFile.parseDate(values.get(0)));
    }

    protected String getAlias(List<String> values) {
        return Joiner.on("\n").join(values);
    }

    private void setPayee(Transaction transaction, List<String> values) {
        transaction.setPayee(payeeMapper.get(getAlias(values)));
    }

    private void setSecurity(Transaction transaction, List<String> values) {
        transaction.setSecurity(securityMapper.get(getAlias(values)));
    }

    protected abstract void setCategory(TransactionDetail detail, ImportField field, List<String> values);

    protected abstract void setTransferAccount(TransactionDetail detail, ImportField field, List<String> values);

    private void setAssetQuantity(TransactionDetail detail, ImportField field, List<String> values) {
        BigDecimal quantity = getAssetQuantity(detail, field, values.get(0));
        if (quantity.compareTo(BigDecimal.ZERO) != 0) detail.setAssetQuantity(quantity);
    }

    protected BigDecimal getAssetQuantity(TransactionDetail detail, ImportField field, String amount) {
        BigDecimal quantity = field.parseAmount(amount);
        return importFile.isNegate(detail.getCategory()) ? quantity.negate() : quantity;
    }

    protected interface DetailValueConsumer {
        void accept(TransactionDetail detail, ImportField field, List<String> values);
    }
}
