#include "transactionservice.h"
#include "database/transactiondetaildao.h"

TransactionService::TransactionService(ConnectionPool *pool, TransactionDao &transactionDao, TransactionDetailDao &detailDao)
    : EntityService{pool, transactionDao}
    , detailDao{detailDao}
{}

QHash<qlonglong, const Transaction *> TransactionService::getAll(qlonglong accountId) {
    Connection conn(connectionPool);
    return dao.getAll(conn.db, accountId);
}

struct TxDetail {
    Transaction *transaction;
    TransactionDetail *detail;
};

class UpdateSession {
    QHash<qlonglong, Transaction*> transactions{};
    QHash<qlonglong, const Transaction*> resultTransactions{};
    QHash<qlonglong, TransactionDetail*> details{};
    QHash<qlonglong, const TransactionDetail*> resultDetails{};

public:
    UpdateSession(const TransactionUpdate &changes) {
        add(changes.updates);
        add(changes.detailUpdates);
    }

    void add(Transaction* tx) {
        removeCopies(tx);
        transactions.insert(tx->id.toLongLong(), tx);
    }

    void add(const QList<Transaction*> transactions) {
        for (auto tx : transactions) add(tx);
    }

    void add(const QList<const Transaction*> transactions) {
        for (auto tx : transactions) {
            removeCopies(tx);
            resultTransactions.insert(tx->id.toLongLong(), tx);
        }
    }

    void add(const QList<TransactionDetail*> details) {
        for (auto detail : details) {
            removeCopies(detail);
            this->details.insert(detail->id.toLongLong(), detail);
        }
    }

    void add(const QList<const TransactionDetail*> details) {
        for (auto detail: details) {
            removeCopies(detail);
            resultDetails.insert(detail->id.toLongLong(), detail);
        }
    }

    Transaction *getTransaction(const Transaction* tx) {
        auto id = tx->id.toLongLong();
        if (transactions.contains(id)) return transactions.value(id);
        if (resultTransactions.contains(id)) delete resultTransactions.take(id);
        auto copy = new Transaction(*tx);
        transactions.insert(id, copy);
        return copy;
    }

    TransactionDetail *getDetail(const TransactionDetail* detail) {
        auto id = detail->id.toLongLong();
        if (details.contains(id)) return details.value(id);
        if (resultDetails.contains(id)) delete resultDetails.take(id);
        auto copy = new TransactionDetail(*detail);
        details.insert(id, copy);
        return copy;
    }

    TransactionsData result() {
        TransactionsData data{};
        for (auto tx : transactions.values()) data.transactions.append(tx);
        for (auto detail : details.values()) data.details.append(detail);
        data.transactions.append(resultTransactions.values());
        data.details.append(resultDetails.values());
        return data;
    }

private:
    void removeCopies(const Transaction *tx) {
        auto id = tx->id.toLongLong();
        const Transaction *t;
        if ((t = transactions.take(id)) && t != tx) delete t;
        if ((t = resultTransactions.take(id)) && t != tx) delete t;
    }

    void removeCopies(const TransactionDetail *detail) {
        auto id = detail->id.toLongLong();
        const TransactionDetail *d;
        if ((d = details.take(id)) && d != detail) delete d;
        if ((d = resultDetails.take(id)) && d != detail) delete d;
    }
};

static void addTransfers(QSqlDatabase &db, TransactionDao &dao, TransactionDetailDao &detailDao, QHash<TransactionDetail*, TxDetail> &detailTransfers, const QString &user, UpdateSession &session) {
    if (!detailTransfers.isEmpty()) {
        QHash<Transaction*, TransactionDetail*> transfers{};
        for (auto [transaction, detail] : detailTransfers.values()) transfers.insert(transaction, detail);
        session.add(dao.add(db, transfers.keys(), user));
        QHash<TransactionDetail*, TransactionDetail*> detailPairs{};
        for (auto [detail, related] : detailTransfers.asKeyValueRange()) {
            detailPairs.insert(detail, related.detail);
            related.detail->relatedDetailId = detail->id;
            related.detail->transactionId = related.transaction->id;
        }
        session.add(detailDao.add(db, transfers.values(), user));
        detailDao.setRelatedDetailIds(db, detailPairs);
    }
}

const TransactionsData TransactionService::update(TransactionUpdate &changes, const QString &user) {
    Connection conn(connectionPool);
    try {
        UpdateSession session{changes}; // TODO return related deletes
        if (!changes.adds.isEmpty()) {
            dao.add(conn.db, changes.adds, user);
            session.add(changes.adds);
        }

        if (!changes.detailAdds.isEmpty()) {
            QHash<TransactionDetail*, TxDetail> transfers{};
            QList<TransactionDetail*> detailAdds{};
            for (auto [transaction, details] : changes.detailAdds.asKeyValueRange()) {
                for (auto detail : std::as_const(details)) {
                    detailAdds.append(detail);
                    detail->transactionId = transaction->id;
                    if (!detail->transferAccountId.isNull()) {
                        auto relatedTransaction = transaction->newTransfer(detail->transferAccountId);
                        auto relatedDetail = detail->newTransfer(transaction->accountId);
                        transfers.insert(detail, {relatedTransaction, relatedDetail});
                    }
                }
            }
            detailDao.add(conn.db, detailAdds, user);
            session.add(detailAdds);
            for (auto [transaction, details] : changes.detailAdds.asKeyValueRange()) {
                auto sessionTx = session.getTransaction(transaction);
                for (auto detail : std::as_const(details)) sessionTx->detailIds.append(detail->id);
            }
            addTransfers(conn.db, dao, detailDao, transfers, user, session);
        }

        if (!changes.updates.isEmpty()) dao.update(conn.db, changes.updates, user);
        if (!changes.detailUpdates.isEmpty()) {
            auto relatedIds = detailDao.getRelatedDetailIds(conn.db, changes.detailUpdates);
            for (auto detail: changes.detailUpdates) {
                if (detail->transferAccountId.isNull()) detail->relatedDetailId = QVariant{};
            }
            session.add(detailDao.update(conn.db, changes.detailUpdates, user));
            for (auto detail : changes.detailUpdates) {
                auto [accountId, relatedDetailId] = relatedIds.value(detail->id.toLongLong());
                if (detail->transferAccountId.isNull()) {
                    if (!relatedDetailId.isNull()) {
                        detailDao.remove(conn.db, relatedDetailId);
                    }
                }
                else if (relatedDetailId.isNull()) {
                    auto transaction = dao.addRelatedTransaction(conn.db, detail, user);
                    session.add(transaction);
                    auto relatedDetail = detail->newTransfer(accountId, transaction->id);
                    session.add(detailDao.add(conn.db, {relatedDetail}, user));
                    transaction->detailIds.append(relatedDetail->id);
                    detailDao.setRelatedDetailIds(conn.db, {{detail, relatedDetail}});
                }
            }
        }

        if (!changes.deletes.isEmpty()) {
            detailDao.removeByTransaction(conn.db, changes.deletes);
            dao.remove(conn.db, changes.deletes);
        }
        if (!changes.detailDeletes.isEmpty()) detailDao.remove(conn.db, changes.detailDeletes);
        dao.removeEmpty(conn.db);
        if (!changes.detailDeletes.isEmpty()) session.add(dao.get(conn.db, TransactionDetail::transactionIds(changes.detailDeletes)).values());
        return session.result();
    } catch(...) {
        conn.db.rollback();
        changes.onError();
        throw;
    }
}
