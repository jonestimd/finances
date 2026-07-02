#include "transactiondetail.h"
#include "service/database/sql.h"
#include <QSqlField>

TransactionDetail::TransactionDetail() {}

TransactionDetail::TransactionDetail(domain_id transactionId) : transactionId{transactionId} {}

TransactionDetail::TransactionDetail(const QSqlRecord &record)
    : BaseDomain{record}
    , transactionId{record.value("tx_id").toLongLong()}
    , categoryId{sql::getInt(record, "tx_category_id")}
    , relatedDetailId{sql::getInt(record, "related_detail_id")}
    , groupId{sql::getInt(record, "tx_group_id")}
    , exchangeAssetId{sql::getInt(record, "exchange_asset_id")}
    , amount{sql::decimalValue(record, "amount").value()}
    , assetQuantity{sql::decimalValue(record, "asset_quantity")}
    , memo{sql::getString(record, "memo")}
    , transferAccountId{sql::getInt(record, "transfer_account_id")}
{}

bool TransactionDetail::isEmpty() const {
    return !categoryId.has_value()
           && !relatedDetailId.has_value()
           && !transferAccountId.has_value()
           && !groupId.has_value()
           && !exchangeAssetId.has_value()
           && memo.isNull()
           && (amount.isNaN() || amount.isZero())
           && (!assetQuantity.has_value() || assetQuantity.value().isZero());
}

TransactionDetail *TransactionDetail::newTransfer(const optional_id &transferAccountId, domain_id transactionId) const {
    auto relatedDetail = new TransactionDetail(*this);
    relatedDetail->id.reset();
    initTransfer(transactionId, *relatedDetail);
    relatedDetail->transferAccountId = transferAccountId;
    return relatedDetail;
}

void TransactionDetail::initTransfer(domain_id transactionId, TransactionDetail &relatedDetail) const {
    relatedDetail.relatedDetailId = id.value();
    relatedDetail.transactionId = transactionId;
    relatedDetail.amount.copyNegate(amount);
    if (assetQuantity.has_value()) {
        relatedDetail.assetQuantity.emplace(QDecNumber{}).copyNegate(assetQuantity.value());
    }
}
