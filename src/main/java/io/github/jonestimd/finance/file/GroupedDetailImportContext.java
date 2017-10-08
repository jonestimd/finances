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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.util.Streams;

/**
 * This class is used to import transactions from a file with one transaction detail in each record and with the detail
 * records grouped by transaction.  Consecutive records with the same date, payee and security are combined into a
 * single transaction and details for the same category within a transaction are merged.
 */
public class GroupedDetailImportContext extends SingleDetailImportContext {
    private final Collection<ImportField> groupingFields;
    private Multimap<ImportField, String> currentGroup;

    public GroupedDetailImportContext(ImportFile importFile, DomainMapper<Payee> payeeMapper,
            DomainMapper<Security> securityMapper, DomainMapper<TransactionCategory> categoryMapper) {
        super(importFile, payeeMapper, securityMapper, categoryMapper);
        groupingFields = Streams.filter(importFile.getFields(), field -> field.getType().isTransaction());
    }

    @Override
    protected Transaction getTransaction(ListMultimap<ImportField, String> record, List<Transaction> transactions) {
        if (currentGroup == null || !isSameGroup(currentGroup, record)) {
            currentGroup = record;
            return super.getTransaction(record, transactions);
        }
        return transactions.get(transactions.size() - 1);
    }

    private boolean isSameGroup(Multimap<ImportField, String> record1, Multimap<ImportField, String> record2) {
        return groupingFields.stream().allMatch(field -> Objects.equals(record1.get(field), record2.get(field)));
    }

    @Override
    protected TransactionDetail getDetail(Transaction transaction, Multimap<ImportField, String> record) {
        if (transaction.getDetails().isEmpty()) return super.getDetail(transaction, record);
        return getValue(record, FieldType.CATEGORY, categoryMapper::get)
                .flatMap(category -> transaction.findFirstDetail(detail -> category.equals(detail.getCategory())))
                .orElseGet(() -> getValue(record, FieldType.TRANSFER_ACCOUNT, importFile::getTransferAccount)
                        .flatMap(account -> transaction.findFirstDetail(detail -> account.equals(detail.getTransferAccount())))
                        .orElseGet(() -> addDetail(transaction)));
    }

    private TransactionDetail addDetail(Transaction transaction) {
        TransactionDetail detail = new TransactionDetail();
        transaction.addDetails(detail);
        return detail;
    }

    @Override
    protected BigDecimal getAmount(TransactionDetail detail, ImportField field, String value) {
        BigDecimal amount = super.getAmount(detail, field, value);
        return detail.getAmount() == null ? amount : detail.getAmount().add(amount);
    }

    @Override
    protected BigDecimal getAssetQuantity(TransactionDetail detail, ImportField field, String amount) {
        BigDecimal quantity = super.getAssetQuantity(detail, field, amount);
        return detail.getAssetQuantity() == null ? quantity : detail.getAssetQuantity().add(quantity);
    }

    private <T> Optional<T> getValue(Multimap<ImportField, String> record, FieldType type, Function<String, T> mapper) {
        Collection<String> values = Multimaps.filterKeys(record, field -> field.getType() == type).values();
        return values.isEmpty() ? Optional.empty() : values.stream().map(mapper).filter(Objects::nonNull).findFirst();
    }
}
