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

import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;

public interface TransactionOperations {

    void updateTransactions(TransactionBulkUpdate transactionUpdate);

    Transaction saveTransaction(TransactionUpdate transactionUpdate);

    <T extends Iterable<Transaction>> T saveTransactions(T transactions);

    TransactionDetail saveDetail(TransactionDetail detail);

    void deleteTransaction(Transaction transaction);

    void moveTransaction(Transaction transaction, Account newAccount);

    List<Transaction> getTransactions(long accountId);

    Transaction findLatestForPayee(long payeeId);

    List<TransactionDetail> findSecuritySalesWithoutLots(String namePrefix, Date saleDate);

    List<TransactionDetail> findPurchasesWithRemainingLots(Account account, Security security, Date date);

    List<SecurityLot> findAvailableLots(TransactionDetail sale);

    void saveSecurityLots(Iterable<? extends SecurityLot> securityLots);

    List<TransactionDetail> findAllDetails(String search);
}