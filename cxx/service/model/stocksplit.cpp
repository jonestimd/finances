#include "stocksplit.h"

StockSplit::StockSplit() {}

StockSplit::StockSplit(const QSqlRecord &record)
    : BaseDomain{record}
    , securityId{record.value("security_id")}
    , date{record.value("date")}
    , sharesIn{record.value("shares_in")}
    , sharesOut{record.value("shares_out")}
{}
