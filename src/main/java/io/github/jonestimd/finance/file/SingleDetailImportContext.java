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

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;

import static io.github.jonestimd.finance.domain.fileimport.FieldType.*;

public class SingleDetailImportContext extends ImportContext {
    private static final Map<FieldType, DetailValueConsumer> detailConsumers = ImmutableMap.of(
        CATEGORY, SingleDetailImportContext::setTransferOrCategory,
        AMOUNT, SingleDetailImportContext::setAmount
    );

    public SingleDetailImportContext(ImportFile importFile, DomainMapper<Payee> payeeMapper, DomainMapper<TransactionCategory> categoryMapper) {
        super(importFile, payeeMapper, categoryMapper);
    }

    private void setTransferOrCategory(TransactionDetail detail, String value, ImportField field) {
        Account account = importFile.getTransferAccount(value);
        if (account != null) {
            detail.setRelatedDetail(new TransactionDetail(account, detail));
        } else {
            detail.setCategory(categoryMapper.get(value));
        }
    }

    private void setAmount(TransactionDetail detail, String amount, ImportField field) {
        detail.setAmount(field.parseAmount(amount));
    }

    @Override
    protected void updateDetails(Transaction transaction, String value, ImportField field) {
        TransactionDetail detail;
        if (transaction.getDetails().isEmpty()) {
            detail = new TransactionDetail();
            transaction.addDetails(detail);
        }
        else {
            detail = transaction.getDetails().get(0);
        }
        updateDetail(detail, value, field);
    }

    private void updateDetail(TransactionDetail detail, String value, ImportField field) {
        DetailValueConsumer consumer = detailConsumers.get(field.getType());
        Preconditions.checkNotNull(consumer, "Invalid field type: %s", field.getType());
        consumer.accept(this, detail, value, field);
    }

    private static interface DetailValueConsumer {
        void accept(SingleDetailImportContext context, TransactionDetail detail, String value, ImportField field);
    }
}
