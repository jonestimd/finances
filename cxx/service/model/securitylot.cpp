#include "securitylot.h"
#include "decimal.h"
#include <QSqlField>

SecurityLot::SecurityLot() {}

SecurityLot::SecurityLot(const QSqlRecord &record)
    : BaseDomain{record}
    , purchaseShares{decimalValue(record, "purchase_shares")}
    , adjustedShares{decimalValue(record, "adjusted_shares")}
    , purchaseDetailId{record.value("purchase_tx_detail_id")}
    , relatedDetailId{record.value("related_tx_detail_id")}
{}
