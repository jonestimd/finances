#ifndef ACCOUNT_H
#define ACCOUNT_H

#include "basedomain.h"
#include "accounttype.h"
#include "decimal.h"
#include <QSqlRecord>
#include <QVariant>

class Account : public BaseDomain {
public:
    QVariant companyId;
    QVariant name;
    QVariant description;
    QVariant type{AccountType::bank.code};
    QVariant accountNumber;
    QVariant closed{false};
    QVariant transactions{0};
    QVariant balance{QVariant::fromValue(QDEC_ZERO)};
    QVariant currency;

    Account();
    Account(QSqlRecord record);

    bool deletable() const;
};

#endif // ACCOUNT_H
