#include "account.h"

#include "sql.h"
#include <QSqlField>

Account::Account() : BaseDomain() {}

Account::Account(QSqlRecord record)
    : BaseDomain(record)
    , companyId{sql::getValue(record, "company_id")}
    , name{record.field("name").value().toString()}
    , description{sql::getValue(record, "description")}
    , type{record.field("type").value()}
    , accountNumber{sql::getValue(record, "account_no")}
    , closed{sql::yesNoValue(record, "closed")}
    , transactions{record.field("transactions").value()}
    , balance{decimalValue(record, "balance")}
    , currency{record.field("currency").value()}
{}

bool Account::deletable() const {
    return transactions.toInt() == 0;
}
