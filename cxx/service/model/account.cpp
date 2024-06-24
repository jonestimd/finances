#include "account.h"
#include "decimal.h"

#include "sql.h"
#include <QSqlField>
#include <QDecNumber.hh>

using sql::getValue;

Account::Account() : BaseDomain() {}

Account::Account(QSqlRecord record) : BaseDomain::BaseDomain(record) {
    companyId = getValue(record, "company_id");
    name = record.field("name").value();
    description = getValue(record, "description");
    type = record.field("type").value();
    accountNumber = getValue(record, "account_no");
    closed = record.field("closed").value().toString() == "Y";
    transactions = record.field("transactions").value();
    balance = decimalValue(record, "balance");
    currency = record.field("currency").value();
}
