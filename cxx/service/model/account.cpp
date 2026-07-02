#include "account.h"

#include "service/database/sql.h"
#include <QSqlField>

Account::Account() : TransactionType{true} {}

Account::Account(const QSqlRecord &record)
    : TransactionType(true, record)
    , companyId{sql::getInt(record, "company_id")}
    , description{sql::getString(record, "description")}
    , type{sql::enumValue(record, "type", AccountType::values)}
    , accountNumber{sql::getString(record, "account_no")}
    , closed{sql::yesNoValue(record, "closed")}
    , transactions{record.field("transactions").value().toInt()}
    , balance{sql::decimalValue(record, "balance").value_or(QDecNumber(0))}
    , currency{sql::getString(record, "currency")}
{}

bool Account::security() const {
    return type->security;
}

bool Account::deletable() const {
    return transactions == 0;
}
