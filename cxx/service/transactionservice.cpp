#include "transactionservice.h"
#include "database/transactiondetaildao.h"

TransactionService::TransactionService(ConnectionPool *pool)
    : EntityService{pool, transactionDao} {}

QHash<qlonglong, const Transaction *> TransactionService::getAll(qlonglong accountId) {
    Connection conn(connectionPool);
    return dao.getAll(conn.db, accountId);
}

struct TxDetail {
    Transaction *transaction;
    TransactionDetail *detail;
};

struct UpdateSession {
    QHash<qlonglong, Transaction*> transactions{};
    QHash<qlonglong, TransactionDetail*> details{};
    TransactionsData result{};

    UpdateSession(const TransactionUpdate &changes) {
        add(changes.updates);
        add(changes.detailUpdates);
    }

    void add(const QList<Transaction*> transactions) {
        for (auto tx : transactions) {
            this->transactions.insert(tx->id.toLongLong(), tx);
            result.transactions.append(tx);
        }
    }

    void add(const QList<TransactionDetail*> details) {
        for (auto detail : details) {
            this->details.insert(detail->id.toLongLong(), detail);
            result.details.append(detail);
        }
    }

    void add(const QList<const TransactionDetail*> details) {
        for (auto detail: details) {
            if (!this->details.contains(detail->id.toLongLong())) result.details.append(detail);
        }
    }

    Transaction *getTransaction(const Transaction* tx) {
        auto id = tx->id.toLongLong();
        if (transactions.contains(id)) return transactions.value(id);
        auto copy = new Transaction(*tx);
        transactions.insert(id, copy);
        result.transactions.append(copy);
        return copy;
    }

    TransactionDetail *getDetail(const TransactionDetail* detail) {
        auto id = detail->id.toLongLong();
        if (details.contains(id)) return details.value(id);
        auto copy = new TransactionDetail(*detail);
        details.insert(id, copy);
        result.details.append(copy);
        return copy;
    }
};

static void addTransfers(QSqlDatabase &db, QHash<TransactionDetail*, TxDetail> &detailTransfers, const QString &user, TransactionsData &updates) {
    if (!detailTransfers.isEmpty()) {
        QHash<Transaction*, TransactionDetail*> transfers{};
        for (auto [transaction, detail] : detailTransfers.values()) transfers.insert(transaction, detail);
        updates.transactions.append(transactionDao.add(db, transfers.keys(), user));
        QHash<TransactionDetail*, TransactionDetail*> detailPairs{};
        for (auto [detail, related] : detailTransfers.asKeyValueRange()) {
            detailPairs.insert(detail, related.detail);
            related.detail->relatedDetailId = detail->id;
            related.detail->transactionId = related.transaction->id;
        }
        updates.details.append(transactionDetailDao.add(db, transfers.values(), user));
        transactionDetailDao.setRelatedDetailIds(db, detailPairs);
    }
}

const TransactionsData TransactionService::update(TransactionUpdate &changes, const QString &user) {
    Connection conn(connectionPool);
    try {
        UpdateSession session{changes}; // TODO return related deletes
        if (!changes.adds.isEmpty()) {
            transactionDao.add(conn.db, changes.adds, user);
            session.add(changes.adds);
        }

        if (!changes.detailAdds.isEmpty()) {
            QHash<TransactionDetail*, TxDetail> transfers{};
            for (auto [transaction, detail] : changes.detailAdds.asKeyValueRange()) {
                detail->transactionId = transaction->id;
                if (!detail->transferAccountId.isNull()) {
                    auto relatedTransaction = transaction->newTransfer(detail->transferAccountId);
                    auto relatedDetail = detail->newTransfer(transaction->accountId);
                    transfers.insert(detail, {relatedTransaction, relatedDetail});
                }
            }
            transactionDetailDao.add(conn.db, changes.detailAdds.values(), user);
            session.add(changes.detailAdds.values());
            for (auto [transaction, detail] : changes.detailAdds.asKeyValueRange()) {
                session.getTransaction(transaction)->detailIds.append(detail->id);
            }
            addTransfers(conn.db, transfers, user, session.result);
        }

        if (!changes.updates.isEmpty()) transactionDao.update(conn.db, changes.updates, user);
        if (!changes.detailUpdates.isEmpty()) {
            auto relatedIds = transactionDetailDao.getRelatedDetailIds(conn.db, changes.detailUpdates);
            session.add(transactionDetailDao.update(conn.db, changes.detailUpdates, user));
            for (auto detail : changes.detailUpdates) {
                auto [accountId, relatedDetailId] = relatedIds.value(detail->id.toLongLong());
                if (detail->transferAccountId.isNull()) {
                    if (!relatedDetailId.isNull()) {
                        transactionDetailDao.remove(conn.db, relatedDetailId);
                    }
                }
                else if (relatedDetailId.isNull()) {
                    auto transaction = transactionDao.addRelatedTransaction(conn.db, detail, user);
                    session.result.transactions.append(transaction);
                    auto relatedDetail = detail->newTransfer(accountId, transaction->id);
                    session.result.details.append(transactionDetailDao.add(conn.db, {relatedDetail}, user));
                    transaction->detailIds.append(relatedDetail->id);
                    transactionDetailDao.setRelatedDetailIds(conn.db, {{detail, relatedDetail}});
                }
            }
        }

        if (!changes.deletes.isEmpty()) {
            transactionDetailDao.removeByTransaction(conn.db, changes.deletes);
            transactionDao.remove(conn.db, changes.deletes);
        }
        if (!changes.detailDeletes.isEmpty()) transactionDetailDao.remove(conn.db, changes.detailDeletes);
        transactionDao.removeEmpty(conn.db);
        return session.result;
    } catch(...) {
        conn.db.rollback();
        changes.onError();
        throw;
    }
}
