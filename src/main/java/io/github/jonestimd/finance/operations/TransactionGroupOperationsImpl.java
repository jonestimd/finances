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
package io.github.jonestimd.finance.operations;

import java.util.List;

import io.github.jonestimd.finance.dao.TransactionGroupDao;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.domain.transaction.TransactionGroupSummary;

public class TransactionGroupOperationsImpl implements TransactionGroupOperations {

    private TransactionGroupDao transactionGroupDao;

    public TransactionGroupOperationsImpl(TransactionGroupDao transactionGroupDao) {
        this.transactionGroupDao = transactionGroupDao;
    }

    public List<TransactionGroup> getAllTransactionGroups() {
        return transactionGroupDao.getAll();
    }

    public TransactionGroup getTransactionGroup(String groupName) {
        return transactionGroupDao.getTransactionGroup(groupName);
    }

    public TransactionGroup getOrCreateTransactionGroup(TransactionGroup group) {
        TransactionGroup existingGroup = transactionGroupDao.getTransactionGroup(group.getName());
        return existingGroup == null ? transactionGroupDao.save(group) : existingGroup;
    }

    public <T extends Iterable<TransactionGroup>> T saveAll(T groups) {
        return transactionGroupDao.saveAll(groups);
    }

    public List<TransactionGroupSummary> getTransactionGroupSummaries() {
        return transactionGroupDao.getTransactionGroupSummaries();
    }

    @Override
    public <T extends Iterable<TransactionGroup>> void deleteAll(T groups) {
        transactionGroupDao.deleteAll(groups);
    }
}