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

import java.math.BigDecimal;
import java.util.List;

import com.google.common.collect.Multimap;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;

/**
 * This class is used to import transactions from a file with one transaction detail in each record.  An import of
 * this type includes separate columns for the category name/alias and the detail amount.
 */
public class SingleDetailImportContext extends ImportContext {
    protected final DomainMapper<TransactionCategory> categoryMapper;

    public SingleDetailImportContext(ImportFile importFile, DomainMapper<Payee> payeeMapper, DomainMapper<Security> securityMapper,
            DomainMapper<TransactionCategory> categoryMapper) {
        super(importFile, payeeMapper, securityMapper);
        this.categoryMapper = categoryMapper;
    }

    @Override
    protected TransactionDetail getDetail(Transaction transaction, Multimap<ImportField, String> record) {
        if (transaction.getDetails().isEmpty()) {
            transaction.addDetails(new TransactionDetail());
        }
        return transaction.getDetails().get(0);
    }

    @Override
    protected DetailValueConsumer getDetailConsumer(ImportField field) {
        return field.getType() == FieldType.AMOUNT ? this::setAmount : super.getDetailConsumer(field);
    }

    private void setAmount(TransactionDetail detail, ImportField field, List<String> values) {
        detail.setAmount(getAmount(detail, field, values.get(0)));
    }

    protected BigDecimal getAmount(TransactionDetail detail, ImportField field, String value) {
        BigDecimal amount = field.parseAmount(value);
        return importFile.isNegate(detail.getCategory()) ? amount.negate() : amount;
    }

    @Override
    protected void setCategory(TransactionDetail detail, ImportField field, List<String> values) {
        Account account = importFile.getTransferAccount(getAlias(values));
        if (account != null) {
            detail.setRelatedDetail(new TransactionDetail(account, detail));
        }
        else if (detail.getCategory() == null) {
            detail.setCategory(categoryMapper.get(getAlias(values)));
        }
    }

    @Override
    protected void setTransferAccount(TransactionDetail detail, ImportField field, List<String> values) {
        detail.setRelatedDetail(new TransactionDetail(importFile.getTransferAccount(getAlias(values)), detail));
    }
}
