#ifndef TRANSACTION_H
#define TRANSACTION_H

#include "basedomain.h"

class Transaction : public BaseDomain {
public:
    QVariant accountId;
    QVariant date;
    QVariant payeeId;
    QVariant securityId;
    QVariant referenceNumber;
    QVariant memo;
    QVariant cleared;
    QList<QVariant> detailIds{};

    Transaction();
    Transaction(const QVariant &accountId);
    Transaction(const QSqlRecord &record);

    bool deletable() const;
};

#endif // TRANSACTION_H
