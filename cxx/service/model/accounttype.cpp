#include "accounttype.h"
#include "account.h"

QHash<const QString, const AccountType*> AccountType::values;

AccountType::AccountType(const char *code, const QString name, bool security)
    : EnumValue(code, name), security{security}
{
    values[code] = this;
}

bool AccountType::isCompatible(const Account *account, const AccountType *type) {
    return !account->id.isValid() || !account->type.isValid() || account->transactions == 0
           || values.value(account->type.toString())->security == type->security;
}

const AccountType AccountType::bank = AccountType("BANK", tr("Bank"), false);
const AccountType AccountType::brokerage = AccountType("BROKERAGE", tr("Brokerage"), true);
const AccountType AccountType::cash = AccountType("CASH", tr("Cash"), false);
const AccountType AccountType::credit = AccountType("CREDIT", tr("Credit"), false);
const AccountType AccountType::loan = AccountType("LOAN", tr("Loan"), false);
const AccountType AccountType::_401k = AccountType("401K", tr("401(K)"), true);
