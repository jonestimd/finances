#include "security.h"
#include "QSqlField"
#include "service/database/sql.h"

Security::Security() : Asset{&AssetType::security} {}

Security::Security(const QSqlRecord &record)
    : Asset(record)
    , securityType{sql::enumValue(record, "security_type", SecurityType::values)}
    , firstAcquired{sql::getDate(record, "first_acquired")}
    , shares{sql::decimalValue(record, "shares").value()}
    , costBasis{sql::decimalValue(record, "cost_basis").value()}
    , dividends{sql::decimalValue(record, "dividends").value()}
{}

AccountSecurityId::AccountSecurityId(domain_id accountId, domain_id securityId)
    : accountId{accountId}, securityId{securityId}
{}

AccountSecurityId::AccountSecurityId(const QSqlRecord &record)
    : accountId{sql::getInt(record, "account_id").value()}
    , securityId{sql::getInt(record, "security_id").value()}
{}

bool AccountSecurityId::operator==(const AccountSecurityId that) const {
    return this->accountId == that.accountId && this->securityId == that.securityId;
}

size_t qHash(const AccountSecurityId &key, size_t seed) {
    return qHashMulti(seed, key.accountId, key.securityId);
}

AccountSecurity::AccountSecurity(const QSqlRecord &record)
    : id{record}
    , firstAcquired{sql::getDate(record, "first_acquired").value()}
    , shares{sql::decimalValue(record, "shares").value()}
    , costBasis{sql::decimalValue(record, "cost_basis").value()}
    , dividends{sql::decimalValue(record, "dividends").value()}
    , transactions{record.value("use_count").toInt()}
{}
