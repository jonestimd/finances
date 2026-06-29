#include "security.h"
#include "QSqlField"
#include "service/database/sql.h"

Security::Security() : Asset{AssetType::security} {}

Security::Security(const QSqlRecord &record)
    : Asset(record)
    , securityType{record.field("security_type").value()}
    , firstAcquired{sql::getValue(record, "first_acquired")}
    , shares{sql::decimalValue(record, "shares").value()}
    , costBasis{sql::decimalValue(record, "cost_basis").value()}
    , dividends{sql::decimalValue(record, "dividends").value()}
{}
