#include "securitylot.h"
#include "service/database/sql.h"
#include <QSqlField>

SecurityLot::SecurityLot() {}

SecurityLot::SecurityLot(const QSqlRecord &record)
    : BaseDomain{record}
    , purchaseShares{sql::decimalValue(record, "purchase_shares").value()}
    , adjustedShares{sql::decimalValue(record, "adjusted_shares").value()}
    , purchaseDetailId{sql::getValue(record, "purchase_tx_detail_id").toLongLong()}
    , relatedDetailId{sql::getValue(record, "related_tx_detail_id").toLongLong()}
{}
