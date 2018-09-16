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
package io.github.jonestimd.finance.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.dao.hibernate.DomainEventRecorder;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.DomainEventHolder;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.operations.TransactionBulkUpdate;
import io.github.jonestimd.finance.operations.TransactionOperations;
import io.github.jonestimd.finance.operations.TransactionUpdate;

public class TransactionServiceImpl implements TransactionService {
    private final TransactionOperations transactionOperations;
    private final DomainEventRecorder eventRecorder;
    
    public TransactionServiceImpl(TransactionOperations transactionOperations, DomainEventRecorder eventRecorder) {
        this.transactionOperations = transactionOperations;
        this.eventRecorder = eventRecorder;
    }

    @Override
    public List<? extends DomainEvent<?, ?>> saveTransaction(Transaction transaction) {
        return saveTransaction(new TransactionUpdate(transaction));
    }

    @Override
    public List<? extends DomainEvent<?, ?>> saveTransaction(TransactionUpdate transactionUpdate) {
        DomainEventHolder eventHolder = eventRecorder.beginRecording();
        transactionOperations.saveTransaction(transactionUpdate);
        return eventHolder.getEvents();
    }

    @Override
    public List<? extends DomainEvent<?, ?>> updateTransactions(TransactionBulkUpdate transactionUpdate) {
        DomainEventHolder eventHolder = eventRecorder.beginRecording();
        transactionOperations.updateTransactions(transactionUpdate);
        return eventHolder.getEvents();
    }

    @Override
    public List<? extends DomainEvent<?, ?>> moveTransaction(Transaction transaction, Account account) {
        DomainEventHolder eventHolder = eventRecorder.beginRecording();
        transactionOperations.moveTransaction(transaction, account);
        return eventHolder.getEvents();
    }

    @Override
    public <T extends Collection<Transaction>> T saveTransactions(T transactions) {
        return transactionOperations.saveTransactions(transactions);
    }

    @Override
    public TransactionDetail saveDetail(TransactionDetail detail) {
        return transactionOperations.saveDetail(detail);
    }

    @Override
    public List<? extends DomainEvent<?, ?>> deleteTransaction(Transaction transaction) {
        DomainEventHolder eventHolder = eventRecorder.beginRecording();
        transactionOperations.deleteTransaction(transaction);
        return eventHolder.getEvents();
    }

    @Override
    public List<Transaction> getTransactions(long accountId) {
        return transactionOperations.getTransactions(accountId);
    }

    @Override
    public Transaction findLatestForPayee(long payeeId) {
        return transactionOperations.findLatestForPayee(payeeId);
    }

    @Override
    public List<TransactionDetail> findSecuritySalesWithoutLots(String namePrefix, Date saleDate) {
        return transactionOperations.findSecuritySalesWithoutLots(namePrefix, saleDate);
    }

    @Override
    public List<TransactionDetail> findPurchasesWithRemainingLots(Account account, Security security, Date date) {
        return transactionOperations.findPurchasesWithRemainingLots(account, security, date);
    }

    @Override
    public List<SecurityLot> findAvailableLots(TransactionDetail sale) {
        return transactionOperations.findAvailableLots(sale);
    }

    @Override
    public void saveSecurityLots(Iterable<? extends SecurityLot> securityLots) {
        transactionOperations.saveSecurityLots(securityLots);
    }

    @Override
    public List<TransactionDetail> findAllDetails(String search) {
        return transactionOperations.findAllDetails(search);
    }
}
