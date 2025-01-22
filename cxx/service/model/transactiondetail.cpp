#include "transactiondetail.h"
#include "decimal.h"
#include "service/database/sql.h"
#include <QSqlField>

TransactionDetail::TransactionDetail(const QVariant &transactionId) : transactionId{transactionId} {}

TransactionDetail::TransactionDetail(const QSqlRecord &record)
    : BaseDomain{record}
    , transactionId{record.field("tx_id").value()}
    , categoryId{sql::getValue(record, "tx_category_id")}
    , relatedDetailId{sql::getValue(record, "related_detail_id")}
    , groupId{sql::getValue(record, "tx_group_id")}
    , exchangeAssetId{sql::getValue(record, "exchange_asset_id")}
    , amount{decimalValue(record, "amount")}
    , assetQuantity{decimalValue(record, "asset_quantity")}
    , memo{sql::getValue(record, "memo")}
{}
