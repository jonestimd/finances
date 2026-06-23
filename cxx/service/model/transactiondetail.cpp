#include "transactiondetail.h"
#include "decimal.h"
#include "service/database/sql.h"
#include <QSqlField>

TransactionDetail::TransactionDetail() {}

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
    , transferAccountId{sql::getValue(record, "transfer_account_id")}
{}

bool TransactionDetail::isEmpty() const {
    return categoryId.isNull()
           && relatedDetailId.isNull()
           && transferAccountId.isNull()
           && groupId.isNull()
           && exchangeAssetId.isNull()
           && memo.isNull()
           && (amount.isNull() || amount.value<QDecNumber>().isZero())
           && (assetQuantity.isNull() || assetQuantity.value<QDecNumber>().isZero());
}

TransactionDetail *TransactionDetail::newTransfer(const QVariant &transferAccountId, const QVariant &transactionId) const {
    auto relatedDetail = new TransactionDetail(*this);
    relatedDetail->id.reset();
    initTransfer(transactionId, *relatedDetail);
    relatedDetail->transferAccountId = transferAccountId;
    return relatedDetail;
}

void TransactionDetail::initTransfer(const QVariant &transactionId, TransactionDetail &relatedDetail) const {
    relatedDetail.relatedDetailId = id.value();
    relatedDetail.transactionId = transactionId;
    relatedDetail.amount = QVariant::fromValue(QDecNumber().copyNegate(amount.value<QDecNumber>()));
    if (!relatedDetail.assetQuantity.isNull()) {
        relatedDetail.assetQuantity = QVariant::fromValue(QDecNumber().copyNegate(assetQuantity.value<QDecNumber>()));
    }
}

QVariantList TransactionDetail::transactionIds(const QList<const TransactionDetail *> details) {
    QVariantList ids{};
    for (auto detail : details) if (!ids.contains(detail->transactionId)) ids.append(detail->transactionId);
    return ids;
}
