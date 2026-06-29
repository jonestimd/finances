#include "securitystore.h"

SecurityStore::SecurityStore(SecurityService *service, StatusMessageStore* messageStore)
    : EntityStore{service, messageStore}
{}

QDecNumber SecurityStore::adjustedShares(const QVariant &securityId, const QDate &date, const QDecNumber &shares) const {
    auto adjusted = shares;
    for (auto split : securitySplits.values(securityId.toLongLong())) {
        if (date <= split->date) {
            adjusted = adjusted.multiply(split->sharesOut);
            adjusted = adjusted.divide(split->sharesIn);
        }
    }
    return adjusted;
}

void SecurityStore::transactionsUpdated(const QList<TransactionChange> changes) {
    if (updateTransactionCounts(changes, &Transaction::securityId)) {
        emit valuesLoaded(ids());
    }
}

void SecurityStore::setValues(const QHash<domain_id, const Security *> values) {
    for (auto i = securitySplits.begin(); i != securitySplits.end(); i = securitySplits.erase(i)) {
        delete i.value();
    }
    if (!values.isEmpty()) {
        auto const splits = service->getSplits();
        for (auto split : splits) securitySplits.insert(split->securityId, split);
    }
    EntityStore::setValues(values);
}
