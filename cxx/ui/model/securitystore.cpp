#include "securitystore.h"

SecurityStore::SecurityStore(SecurityService *service)
    : EntityStore{service}
{}

QDecNumber SecurityStore::adjustedShares(const QVariant &securityId, const QDate &date, const QDecNumber &shares) const {
    auto adjusted = shares;
    for (auto split : securitySplits.values(securityId.toLongLong())) {
        if (date <= split->date.value<QDate>()) {
            adjusted = adjusted.multiply(split->sharesOut.value<QDecNumber>());
            adjusted = adjusted.divide(split->sharesIn.value<QDecNumber>());
        }
    }
    return adjusted;
}

void SecurityStore::setValues(const QHash<qlonglong, const Security *> values) {
    for (auto i = securitySplits.begin(); i != securitySplits.end(); i = securitySplits.erase(i)) {
        delete i.value();
    }
    if (!values.isEmpty()) {
        auto const splits = service->getSplits();
        for (auto split : splits) securitySplits.insert(split->securityId.toLongLong(), split);
    }
    EntityStore::setValues(values);
}
