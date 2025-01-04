#ifndef ACCOUNT_TYPE_H
#define ACCOUNT_TYPE_H

#include "service/model/basedomain.h"
#include <QHash>
#include <QObject>
#include <QString>

class Account;

class AccountType : public EnumValue {
    Q_OBJECT
    AccountType(const char *code, const QString name, bool security);
public:
    const bool security;

    static const AccountType bank;
    static const AccountType brokerage;
    static const AccountType cash;
    static const AccountType credit;
    static const AccountType loan;
    static const AccountType _401k;

    static QHash<QString, const AccountType*> values;

    static bool isCompatible(const Account* account, const AccountType *type);
};

#endif // ACCOUNT_TYPE_H
