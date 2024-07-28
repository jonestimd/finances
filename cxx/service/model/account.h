#ifndef ACCOUNT_H
#define ACCOUNT_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class Account : public BaseDomain {
public:
    QVariant companyId;
    QVariant name;
    QVariant description;
    QVariant type;
    QVariant accountNumber;
    QVariant closed;
    QVariant transactions;
    QVariant balance;
    QVariant currency;

    Account();
    Account(QSqlRecord record);

    bool deletable() const;
};

#endif // ACCOUNT_H
