#include "account.h"
#include "accounttype.h"
#include "decimal.h"

#include "sql.h"
#include <QSqlField>
#include <QDecNumber.hh>

using sql::getValue;

Account::Account() : BaseDomain()
    , closed{false}
    , type{AccountType::bank.code}
    , transactions{0}
    , balance{QVariant::fromValue(QDEC_ZERO)} {}

Account::Account(QSqlRecord record) : BaseDomain::BaseDomain(record) {
    companyId = getValue(record, "company_id");
    name = record.field("name").value().toString();
    description = getValue(record, "description").toString();
    type = record.field("type").value().toString();
    accountNumber = getValue(record, "account_no").toString();
    closed = record.field("closed").value().toString() == "Y";
    transactions = record.field("transactions").value();
    balance = decimalValue(record, "balance");
    currency = record.field("currency").value();
}

bool Account::deletable() const {
    return transactions.toInt() == 0;
}
