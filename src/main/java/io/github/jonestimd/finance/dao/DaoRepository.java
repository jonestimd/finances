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
package io.github.jonestimd.finance.dao;

import java.sql.SQLException;
import java.util.List;

import io.github.jonestimd.finance.dao.hibernate.DomainEventRecorder;

public interface DaoRepository {
    void generateSchema(List<String> postCreateScript) throws SQLException;

    <I,T extends I> I transactional(T target, Class<I> iface);

    void doInTransaction(Runnable work);

    CompanyDao getCompanyDao();

    AccountDao getAccountDao();

    PayeeDao getPayeeDao();

    TransactionCategoryDao getTransactionCategoryDao();

    TransactionGroupDao getTransactionGroupDao();

    TransactionDao getTransactionDao();

    TransactionDetailDao getTransactionDetailDao();

    CurrencyDao getCurrencyDao();

    SecurityDao getSecurityDao();

    StockSplitDao getStockSplitDao();

    SecurityLotDao getSecurityLotDao();

    ImportFileDao getImportFileDao();

    DomainEventRecorder getDomainEventRecorder();
}