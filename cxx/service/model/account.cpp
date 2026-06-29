#include "account.h"

#include "service/database/sql.h"
#include <QSqlField>

Account::Account() : TransactionType{true} {}

Account::Account(const QSqlRecord &record)
    : TransactionType(true, record)
    , companyId{sql::getInt(record, "company_id")}
    , description{sql::getValue(record, "description")}
    , type{sql::getValue(record, "type", AccountType::bank.code)}
    , accountNumber{sql::getValue(record, "account_no")}
    , closed{sql::yesNoValue(record, "closed")}
    , transactions{record.field("transactions").value().toInt()}
    , balance{sql::decimalValue(record, "balance").value_or(QDecNumber(0))}
    , currency{record.field("currency").value()}
{}

bool Account::security() const {
    return AccountType::values.value(type.toString())->security;
}

bool Account::deletable() const {
    return transactions == 0;
}
