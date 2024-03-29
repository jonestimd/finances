// The MIT License (MIT)
//
// Copyright (c) 2022 Tim Jones
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
package io.github.jonestimd.finance.operations;

import java.util.List;

import io.github.jonestimd.finance.dao.PayeeDao;
import io.github.jonestimd.finance.dao.TransactionDao;
import io.github.jonestimd.finance.domain.UniqueName;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.PayeeSummary;

public class PayeeOperationsImpl implements PayeeOperations {
    private final PayeeDao payeeDao;
    private final TransactionDao transactionDao;

    public PayeeOperationsImpl(PayeeDao payeeDao, TransactionDao transactionDao) {
        this.payeeDao = payeeDao;
        this.transactionDao = transactionDao;
    }

    public Payee getPayee(String name) {
        return payeeDao.getPayee(name);
    }

    public List<Payee> getAllPayees() {
        List<Payee> payees = payeeDao.getAll();
        payees.sort(UniqueName.BY_NAME);
        return payees;
    }

    public List<PayeeSummary> getPayeeSummaries() {
        return payeeDao.getPayeeSummaries();
    }

    public Payee createPayee(String name) {
        Payee payee = new Payee();
        payee.setName(name);
        return payeeDao.save(payee);
    }

    public <T extends Iterable<Payee>> T saveAll(T payees) {
        return payeeDao.saveAll(payees);
    }

    @Override
    public <T extends Iterable<Payee>> void deleteAll(T payees) {
        payeeDao.deleteAll(payees);
    }

    @Override
    public List<Payee> merge(List<Payee> toReplace, Payee payee) {
        transactionDao.replacePayee(toReplace, payee);
        payeeDao.deleteAll(toReplace);
        return toReplace;
    }
}