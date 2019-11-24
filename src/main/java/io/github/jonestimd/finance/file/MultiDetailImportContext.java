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

import java.util.List;

import com.google.common.collect.Multimap;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;

/**
 * This class is used to import transactions from a file with multiple transaction details in each record.  An import
 * of this type includes category specific columns that contain the amount for the associated category.
 */
public class MultiDetailImportContext extends ImportContext {
    public MultiDetailImportContext(ImportFile importFile, DomainMapper<Payee> payeeMapper, DomainMapper<Security> securityMapper) {
        super(importFile, payeeMapper, securityMapper);
    }

    @Override
    protected void setCategory(TransactionDetail detail, ImportField field, List<String> values) {
        field.updateDetail(detail, values.get(0));
    }

    @Override
    protected void setTransferAccount(TransactionDetail detail, ImportField field, List<String> values) {
        field.updateDetail(detail, values.get(0));
    }

    @Override
    protected TransactionDetail getDetail(Transaction transaction, Multimap<ImportField, String> record) {
        TransactionDetail detail = new TransactionDetail();
        transaction.addDetails(detail);
        return detail;
    }
}
