// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.github.jonestimd.finance.dao.DaoRepository;
import io.github.jonestimd.finance.dao.PayeeDao;
import io.github.jonestimd.finance.dao.SecurityDao;
import io.github.jonestimd.finance.dao.SecurityLotDao;
import io.github.jonestimd.finance.dao.TransactionCategoryDao;
import io.github.jonestimd.finance.dao.TransactionDao;
import io.github.jonestimd.finance.dao.TransactionDetailDao;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.SecurityAction;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.util.JavaPredicates.*;

public class TransactionOperationsImpl implements TransactionOperations {
    private final TransactionDao transactionDao;
    private final TransactionDetailDao transactionDetailDao;
    private final PayeeDao payeeDao;
    private final SecurityDao securityDao;
    private final SecurityLotDao securityLotDao;
    private final TransactionCategoryDao categoryDao;

    public TransactionOperationsImpl(DaoRepository daoRepository) {
        this.transactionDao = daoRepository.getTransactionDao();
        this.transactionDetailDao = daoRepository.getTransactionDetailDao();
        this.payeeDao = daoRepository.getPayeeDao();
        this.securityDao = daoRepository.getSecurityDao();
        this.securityLotDao = daoRepository.getSecurityLotDao();
        this.categoryDao = daoRepository.getTransactionCategoryDao();
    }

    public <T extends Iterable<Transaction>> T saveTransactions(T transactions) {
        return transactionDao.saveAll(transactions);
    }

    public TransactionDetail saveDetail(TransactionDetail detail) {
        return transactionDetailDao.save(detail);
    }

    @Override
    public void moveTransaction(Transaction transaction, Account newAccount) {
        Account oldAccount = transaction.getAccount();
        transaction.setAccount(newAccount);
        transaction.getDetails().stream().filter(isTransferToAccount(newAccount)).forEach(setTransferAccount(oldAccount));
        transactionDao.merge(transaction);
    }

    private java.util.function.Predicate<TransactionDetail> isTransferToAccount(Account account) {
        return detail -> detail.isTransfer() && detail.getRelatedDetail().getTransaction().getAccount().getId().equals(account.getId());
    }

    private Consumer<TransactionDetail> setTransferAccount(Account oldAccount) {
        return detail -> detail.getRelatedDetail().getTransaction().setAccount(oldAccount);
    }

    public void deleteTransaction(Transaction transaction) {
        for (TransactionDetail detail : transaction.getDetails()) {
            if (detail.isTransfer()) {
                if (detail.getRelatedDetail().getTransaction().getDetails().size() == 1) {
                    transactionDao.delete(detail.getRelatedDetail().getTransaction());
                } else {
                    detail.getRelatedDetail().setTransaction(null);
                }
            }
            if (SecurityAction.SELL.isSameCode(detail.getCategory())) { // TODO what about purchases?
                securityLotDao.deleteSaleLots(detail);
            }
        }
        transactionDao.delete(transaction);
    }

    @Override
    public void updateTransactions(TransactionBulkUpdate transactionUpdate) {
        transactionUpdate.getDeletes().forEach(this::deleteTransaction);
        transactionUpdate.getUpdates().forEach(this::saveTransaction);
    }

    public Transaction saveTransaction(TransactionUpdate transactionUpdate) {
        Transaction transaction = transactionUpdate.getTransaction();
        persistPayee(transaction.getPayee());
        persistSecurity(transaction.getSecurity());
        transactionUpdate.getNewCategories().forEach(categoryDao::save);
        transaction.getDetails().removeIf(TransactionDetail::isUnsavedAndEmpty);
        if (transaction.getId() != null) {
            Transaction persisted = transactionDao.merge(transaction);
            for (TransactionDetail detail : transactionUpdate.getDeletes()) {
                deleteDetail(persisted.getDetail(detail.getId()));
            }
            for (TransactionDetail detail : transactionDetailDao.findOrphanTransfers()) {
                detail.setRelatedDetail(null);
                deleteDetail(detail);
            }
            return persisted;
        }
        return transactionDao.save(transaction);
    }

    private void persistPayee(Payee payee) {
        if (payee != null && payee.isNew()) {
            payeeDao.save(payee);
        }
    }

    private void persistSecurity(Security security) {
        if (security != null && security.isNew()) {
            securityDao.save(security);
        }
    }

    private void deleteDetail(TransactionDetail detail) {
        securityLotDao.deleteSaleLots(detail);
        if (detail.getTransaction().getDetails().size() == 1) {
            transactionDao.delete(detail.getTransaction());
        } else {
            detail.setTransaction(null);
            if (detail.isTransfer()) {
                detail.getRelatedDetail().setRelatedDetail(null);
                deleteDetail(detail.getRelatedDetail());
                detail.setRelatedDetail(null);
            }
            transactionDetailDao.delete(detail);
        }
    }

    public List<Transaction> getTransactions(long accountId) {
        return transactionDao.getTransactions(accountId);
    }

    @Override
    public Transaction findLatestForPayee(long payeeId) {
        return transactionDao.findLatestForPayee(payeeId);
    }

    public List<TransactionDetail> findSecuritySalesWithoutLots(String namePrefix, Date saleDate) {
        return transactionDetailDao.findSecuritySalesWithoutLots(namePrefix, saleDate);
    }

    public List<TransactionDetail> findPurchasesWithRemainingLots(Account account, Security security, Date date) {
        return transactionDetailDao.findPurchasesWithRemainingShares(account, security, date);
    }

    @Override
    public List<SecurityLot> findAvailableLots(TransactionDetail sale) {
        List<SecurityLot> lots = securityLotDao.findBySale(sale);
        List<TransactionDetail> purchases = Streams.map(lots, SecurityLot::getPurchase);
        transactionDetailDao.findAvailablePurchaseShares(sale).stream().filter(not(purchases::contains))
                .forEach(purchase -> lots.add(new SecurityLot(purchase, sale, BigDecimal.ZERO)));
        return lots;
    }

    @Override
    public void saveSecurityLots(Iterable<? extends SecurityLot> securityLots) {
        securityLotDao.deleteAll(Streams.of(securityLots).filter(lot -> !lot.isNew() && lot.isEmpty()));
        securityLotDao.saveAll(Streams.of(securityLots).filter(lot -> !lot.isEmpty()));
    }

    @Override
    public List<TransactionDetail> findAllDetails(String search) {
        List<TransactionCategory> categories = categoryDao.findByPartialCode(search);
        List<Long> categoryIds = categories.stream().flatMap(TransactionCategory::getSubcategoryIds).collect(Collectors.toList());
        Set<TransactionDetail> details = new HashSet<>();
        details.addAll(transactionDetailDao.findByCategoryIds(categoryIds));
        details.addAll(transactionDetailDao.findByString(search));
        return Lists.newArrayList(details);
    }
}