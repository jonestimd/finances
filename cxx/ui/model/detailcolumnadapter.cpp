#include "detailcolumnadapter.h"
#include "transactiontablemodel.h"
#include "ui/model/formats.h"
#include "ui/validation/detailvalidator.h"

// TransactionTypeColumnAdapter //

TransactionTypeColumnAdapter::TransactionTypeColumnAdapter(const QString &title, DataStore *dataStore)
    : ColumnAdapter(title, &TransactionDetail::categoryId, true)
    , dataStore{dataStore}
{}

static int count(0);

QVariant TransactionTypeColumnAdapter::value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const {
    QVariant value = ColumnAdapter::value(row, index, getId(current), Qt::DisplayRole);
    const auto typeId = value.value<TransactionTypeId>();
    switch (role) {
    case Qt::DisplayRole:
        if (current.isValid() && current.isNull()) return "";
        if (value.isValid() && !value.isNull()) {
            if (typeId.transfer) return dataStore->accountStore->qualifiedName(typeId.id, ':'); // .prepend("\u279c ");
            if (typeId.id.isValid()) return dataStore->categoryStore->displayName(typeId.id.toLongLong());
        }
        break;
    case Qt::EditRole:
        if (current.isValid()) return current;
        if (value.isValid() && !value.isNull()) {
            if (typeId.transfer) return QVariant::fromValue<const NamedEntity*>(dataStore->accountStore->value(typeId.id));
            return QVariant::fromValue<const NamedEntity*>(dataStore->categoryStore->value(typeId.id));
        }
        break;
    case finances::OptionsRole:
        return QVariant::fromValue(getOptions());
    case Qt::DecorationRole:
        return value.isValid() && typeId.transfer ? finances::ArrowRight : finances::None;
    }
    return QVariant{};
}

QVariant TransactionTypeColumnAdapter::fieldValue(const TransactionDetail *row) const {
    if (!row->relatedDetailId.isNull()) {
        auto rd = dataStore->transactionStore->detailStore.value(row->relatedDetailId);
        auto rx = dataStore->transactionStore->value(rd->transactionId);
        return QVariant::fromValue(TransactionTypeId(true, rx->accountId));
    }
    return QVariant::fromValue(TransactionTypeId(false, ColumnAdapter::fieldValue(row)));
}

void TransactionTypeColumnAdapter::setValue(TransactionDetail *row, QVariant value) const {
    qCritical() << "setting value on detail instead of detail model";
    // auto typeId = value.value<TransactionTypeId>();
    // if (typeId.transfer) {
    //     if (row->relatedDetailId.isValid()) {
    //         auto rd = dataStore->transactionStore->detailStore.value(row->relatedDetailId);
    //         auto rx = new Transaction(*dataStore->transactionStore->value(rd->transactionId));
    //         if (rx->detailIds.length() > 1) qWarning("moving multi-detail tx: %lld", rx->id.toLongLong());
    //         rx->accountId = typeId.id;
    //         // TODO where to put rx?
    //     }
    //     // TODO create related detail and tx?
    // } else if (typeId.id.isValid()) {
    //     ColumnAdapter::setValue(row, typeId.id);
    //     // TODO remove related detail
    // } else {
    //     // TODO remove category & related detail
    // }
}

void TransactionTypeColumnAdapter::setValue(TransactionDetailUpdate *model, QVariant value) const {
    auto typeId = value.value<TransactionTypeId>();
    if (typeId.transfer) {
        model->transferAccountId = typeId.id;
        model->detail->categoryId = QVariant{};
    } else {
        model->detail->categoryId = typeId.id;
        model->transferAccountId = QVariant{};
    }
}

QVariant TransactionTypeColumnAdapter::getId(const QVariant &value) const {
    auto tt = static_cast<const TransactionType*>(value.value<const NamedEntity*>());
    return tt ? QVariant::fromValue(TransactionTypeId(tt)) : QVariant{};
}

QString TransactionTypeColumnAdapter::optionText(const NamedEntity* option) const {
    auto type = static_cast<const TransactionType*>(option);
    if (type->transfer) return dataStore->accountStore->qualifiedName(option->id, ':');
    return dataStore->categoryStore->displayName(option->id.toLongLong());
}

ComboBoxModel *TransactionTypeColumnAdapter::getOptions() const {
    QList<const NamedEntity*> options;
    for (auto category: dataStore->categoryStore->values()) options.append(category);
    for (auto account: dataStore->accountStore->values()) options.append(account);
    return new ComboBoxModel(options, std::bind_front(&TransactionTypeColumnAdapter::optionText, this));
}

// SharesColumnAdapter //

SharesColumnAdapter::SharesColumnAdapter(const QString &title, const TransactionTableModel *model)
    : AmountColumnAdapter{title, &TransactionDetail::assetQuantity, securityShares, true,
        new SharesValidatorFactory(model->payeeColumn, model->securityColumn, model->subtotalColumn)}
{}

// DetailAmountColumnAdapter //

DetailAmountColumnAdapter::DetailAmountColumnAdapter(const QString &title)
    : AmountColumnAdapter{title, &TransactionDetail::amount, dollarFormat, true, new DetailAmountValidatorFactory()}
{}

// EmptyColumnAdapter //

EmptyColumnAdapter::EmptyColumnAdapter() : ColumnAdapter{"", nullptr, false} {}

QVariant EmptyColumnAdapter::value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const {
    return QVariant{};
}
