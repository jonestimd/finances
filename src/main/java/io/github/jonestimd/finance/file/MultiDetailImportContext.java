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
import java.util.function.BiConsumer;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;

import static io.github.jonestimd.finance.domain.fileimport.FieldType.*;

public class MultiDetailImportContext extends ImportContext {
    private final Map<FieldType, BiConsumer<TransactionDetail, ImportField>> detailConsumers = ImmutableMap.of(
        CATEGORY, this::setCategory,
        TRANSFER_ACCOUNT, this::setTransferAccount
    );

    public MultiDetailImportContext(ImportFile importFile, DomainMapper<Payee> payeeMapper, DomainMapper<TransactionCategory> categoryMapper) {
        super(importFile, payeeMapper, categoryMapper);
    }

    private void setCategory(TransactionDetail detail, ImportField field) {
        detail.setCategory(categoryMapper.get(field.getLabel()));
    }

    private void setTransferAccount(TransactionDetail detail, ImportField field) {
        detail.setRelatedDetail(new TransactionDetail(importFile.getTransferAccount(field.getLabel()), detail));
    }

    @Override
    protected void updateDetails(Transaction transaction, String value, ImportField field) {
        TransactionDetail detail = new TransactionDetail();
        transaction.addDetails(detail);
        updateDetail(detail, value, field);
    }

    private void updateDetail(TransactionDetail detail, String value, ImportField field) {
        BiConsumer<TransactionDetail, ImportField> consumer = detailConsumers.get(field.getType());
        Preconditions.checkNotNull(consumer, "Invalid file type: %s", field.getType());
        consumer.accept(detail, field);
        field.updateDetail(detail, value);
    }
}
