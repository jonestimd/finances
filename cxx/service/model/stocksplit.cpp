#include "stocksplit.h"
#include "decimal.h"

StockSplit::StockSplit() {}

StockSplit::StockSplit(const QSqlRecord &record)
    : BaseDomain{record}
    , securityId{record.value("security_id")}
    , date{record.value("date")}
    , sharesIn{decimalValue(record, "shares_in")}
    , sharesOut{decimalValue(record, "shares_out")}
{}
