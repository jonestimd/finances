#include "transactiondetail.h"
#include "decimal.h"
#include "service/database/sql.h"
#include <QSqlField>

TransactionDetail::TransactionDetail() {}

TransactionDetail::TransactionDetail(domain_id transactionId) : transactionId{transactionId} {}

TransactionDetail::TransactionDetail(const QSqlRecord &record)
    : BaseDomain{record}
    , transactionId{record.field("tx_id").value().toLongLong()}
    , categoryId{sql::getInt(record, "tx_category_id")}
    , relatedDetailId{sql::getValue(record, "related_detail_id")}
    , groupId{sql::getValue(record, "tx_group_id")}
    , exchangeAssetId{sql::getValue(record, "exchange_asset_id")}
    , amount{decimalValue(record, "amount")}
    , assetQuantity{decimalValue(record, "asset_quantity")}
    , memo{sql::getValue(record, "memo")}
    , transferAccountId{sql::getInt(record, "transfer_account_id")}
{}

bool TransactionDetail::isEmpty() const {
    return !categoryId.has_value()
           && relatedDetailId.isNull()
           && !transferAccountId.has_value()
           && groupId.isNull()
           && exchangeAssetId.isNull()
           && memo.isNull()
           && (amount.isNull() || amount.value<QDecNumber>().isZero())
           && (assetQuantity.isNull() || assetQuantity.value<QDecNumber>().isZero());
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
