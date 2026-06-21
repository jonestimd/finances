#ifndef ACCOUNT_H
#define ACCOUNT_H

#include "basedomain.h"
#include "accounttype.h"
#include "decimal.h"
#include <QSqlRecord>
#include <QVariant>

class Account : public TransactionType {
public:
    QVariant companyId;
    QVariant description;
    QVariant type{AccountType::bank.code};
    QVariant accountNumber;
    QVariant closed{false};
    mutable QVariant transactions{0};
    QVariant balance{QVariant::fromValue(QDEC_ZERO)};
    QVariant currency;

    Account();
    Account(const QSqlRecord &record);

    bool security() const;
    bool deletable() const;
};

#endif // ACCOUNT_H
