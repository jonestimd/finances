#include "security.h"
#include "QSqlField"
#include "service/model/decimal.h"
#include "service/database/sql.h"

Security::Security() : Asset{AssetType::security} {}

Security::Security(const QSqlRecord &record)
    : Asset(record)
    , securityType{record.field("security_type").value()}
    , firstAcquired{sql::getValue(record, "first_acquired")}
    , shares{decimalValue(record, "shares")}
    , costBasis{decimalValue(record, "cost_basis")}
    , dividends{decimalValue(record, "dividends")}
{}
