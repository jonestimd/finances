#include "accounttype.h"

QHash<QString, const AccountType*> AccountType::values;

AccountType::AccountType(const char *code, const char *name, bool security)
    : EnumValue(code, name), security{security}
{
    values[code] = this;
}

bool AccountType::isCompatible(const Account *account, const AccountType *type) {
    return !account->id.isValid() || !account->type.isValid() || account->transactions == 0
           || values.value(account->type.toString())->security == type->security;
}

const AccountType AccountType::bank = AccountType("BANK", "Bank", false);
const AccountType AccountType::brokerage = AccountType("BROKERAGE", "Brokerage", true);
const AccountType AccountType::cash = AccountType("CASH", "Cash", false);
const AccountType AccountType::credit = AccountType("CREDIT", "Credit", false);
const AccountType AccountType::loan = AccountType("LOAN", "Loan", false);
const AccountType AccountType::_401k = AccountType("401K", "401(K)", true);
