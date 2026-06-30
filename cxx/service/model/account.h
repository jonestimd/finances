#ifndef ACCOUNT_H
#define ACCOUNT_H

#include "basedomain.h"
#include "accounttype.h"
#include <QSqlRecord>
#include <QVariant>

class Account : public TransactionType {
public:
    optional_id companyId;
    QString description;
    const AccountType* type{&AccountType::bank};
    QString accountNumber;
    bool closed{false};
    mutable int transactions{0};
    QDecNumber balance{0};
    QString currency;

    Account();
    Account(const QSqlRecord &record);

    bool security() const;
    bool deletable() const;
};

#endif // ACCOUNT_H
