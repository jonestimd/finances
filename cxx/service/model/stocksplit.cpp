#include "stocksplit.h"
#include "service/database/sql.h"

StockSplit::StockSplit() {}

StockSplit::StockSplit(const QSqlRecord &record)
    : BaseDomain{record}
    , securityId{sql::getValue(record, "security_id").toLongLong()}
    , date{record.value("date").toDate()}
    , sharesIn{sql::decimalValue(record, "shares_in").value()}
    , sharesOut{sql::decimalValue(record, "shares_out").value()}
{}
