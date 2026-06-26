#include "transactionservice.h"
#include "database/transactiondetaildao.h"

TransactionService::TransactionService(ConnectionPool *pool, TransactionDao &transactionDao, TransactionDetailDao &detailDao)
    : EntityService{pool, transactionDao}
    , detailDao{detailDao}
{}

QHash<domain_id, const Transaction *> TransactionService::getAll(domain_id accountId) {
    Connection conn(connectionPool);
    return dao.getAll(conn.db, accountId);
}

struct TxDetail {
    Transaction *transaction;
    TransactionDetail *detail;
};

class UpdateSession {
    QHash<domain_id, Transaction*> transactions{};
    QList<const Transaction*> resultTransactions{};
    QHash<domain_id, TransactionDetail*> details{};
    QList<const TransactionDetail*> resultDetails{};

public:
    UpdateSession(const TransactionUpdate &changes) {
        for (auto tx : std::as_const(changes.updates)) add(tx);
    }

    ~UpdateSession() {
        qDeleteAll(transactions);
        transactions.clear();
        qDeleteAll(resultTransactions);
        resultTransactions.clear();
    }

    void add(Transaction* tx) {
        transactions.insert(tx->id.value(), tx);
    }

    void add(const QList<const Transaction*> transactions) {
        for (auto tx : transactions) resultTransactions.append(tx);
    }

    void add(const QList<TransactionDetail*> details) {
        for (auto detail : details) {
            this->details.insert(detail->id.value(), detail);
        }
    }

    void add(const QList<const TransactionDetail*> details) {
        for (auto detail: details) resultDetails.append(detail);
    }

    std::optional<Transaction*> getTransaction(const QVariant &id) {
        if (transactions.contains(id.toLongLong())) return transactions.value(id.toLongLong());
        return std::nullopt;
    }

    TransactionsData result(const QList<TransactionDetail*> detailAdds, const QList<domain_id> deletedIds, const QList<domain_id> deletedDetailIds) {
        TransactionsData data{resultTransactions, resultDetails, deletedIds, deletedDetailIds};
        resultTransactions.clear();
        for (auto i = transactions.cbegin(); i != transactions.cend(); i++) data.transactions.append(i.value());
        transactions.clear();
        for (auto i = details.cbegin(); i != details.cend(); i++) data.details.append(i.value());
        for (auto detail : detailAdds) data.details.append(detail);
        return data;
    }
};

const TransactionsData TransactionService::update(TransactionUpdate &changes, const QString &user) {
    Connection conn(connectionPool);
    try {
        UpdateSession session{changes};
        QList<domain_id> deletedDetailIds;
        QList<TransactionDetail*> detailAdds{changes.detailAdds};
        if (!changes.adds.isEmpty()) {
            dao.add(conn.db, changes.adds, user);
            for (auto tx : std::as_const(changes.adds)) {
                session.add(new Transaction(*tx));
                detailAdds.append(tx->details);
                tx->details.clear(); // take ownership from the PendingTransaction
            }
        }

        QList<domain_id> txIds{};
        if (!detailAdds.isEmpty()) {
            QHash<TransactionDetail*, domain_id> relatedIds{};
            detailDao.add(conn.db, detailAdds, user);
            for (auto detail : std::as_const(detailAdds)) {
                if (auto tx = session.getTransaction(detail->transactionId)) (*tx)->detailIds.append(detail->id.value());
                else txIds.append(detail->transactionId);
                if (detail->transferAccountId.has_value()) {
                    auto relatedTransaction = dao.addRelatedTransaction(conn.db, detail, user);
                    auto relatedDetail = detailDao.addRelatedDetail(conn.db, relatedTransaction->id.value(), detail, user);
                    relatedTransaction->detailIds.append(relatedDetail->id.value());
                    relatedIds.insert(detail, relatedDetail->id.value());
                    session.add(relatedTransaction);
                    session.add({relatedDetail}); // TODO delete on error
                }
            }
            if (!relatedIds.isEmpty()) detailDao.setRelatedDetailIds(conn.db, relatedIds);
        }

        if (!changes.updates.isEmpty()) dao.update(conn.db, changes.updates, user);
        if (!changes.detailUpdates.isEmpty()) {
            auto relatedDetailIds = detailDao.getRelatedDetailIds(conn.db, changes.detailUpdates);
            for (auto detail : changes.detailUpdates) {
                if (!detail->transferAccountId.has_value()) detail->relatedDetailId = QVariant{};
            }
            session.add(detailDao.update(conn.db, changes.detailUpdates, user)); // TODO delete related details on error
            for (auto detail : changes.detailUpdates) {
                auto savedIds = relatedDetailIds.value(detail->id.value());
                if (!detail->transferAccountId.has_value()) {
                    if (savedIds.relatedDetailId.has_value()) {
                        deletedDetailIds.append(savedIds.relatedDetailId.value());
                        detailDao.remove(conn.db, savedIds.relatedDetailId.value());
                    }
                } else if (!savedIds.relatedDetailId.has_value()) {
                    auto transaction = dao.addRelatedTransaction(conn.db, detail, user);
                    session.add(transaction);
                    auto relatedDetail = detail->newTransfer(savedIds.accountId, transaction->id.value());
                    session.add(detailDao.add(conn.db, {relatedDetail}, user)); // TODO delete on error
                    transaction->detailIds.append(relatedDetail->id.value());
                    detailDao.setRelatedDetailIds(conn.db, {{detail, relatedDetail->id.value()}});
                } else if (detail->transferAccountId.value() != savedIds.transferAccountId.value()) {
                    dao.setAccountId(conn.db, savedIds.relatedTransactionId.value(), savedIds.transferAccountId.value(), detail->transferAccountId.value(), user);
                    txIds.append(savedIds.relatedTransactionId.value());
                }
            }
        }

        if (!changes.deletes.isEmpty()) {
            deletedDetailIds.append(detailDao.removeByTransaction(conn.db, changes.deletes, txIds));
            dao.remove(conn.db, changes.deletes);
        }
        if (!changes.detailDeletes.isEmpty()) detailDao.remove(conn.db, changes.detailDeletes);
        auto deletedIds = dao.removeEmpty(conn.db);
        if (!changes.detailDeletes.isEmpty()) {
            for (auto detail : changes.detailDeletes) {
                if (auto tx = session.getTransaction(detail->transactionId)) (*tx)->detailIds.removeAll(detail->id);
                else txIds.append(detail->transactionId);
            }
        }
        if (!txIds.isEmpty()) session.add(dao.get(conn.db, txIds).values());
        return session.result(detailAdds, deletedIds, deletedDetailIds);
    } catch(...) {
        conn.db.rollback();
        changes.onError();
        throw;
    }
}
