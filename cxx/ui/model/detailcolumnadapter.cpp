#include "detailcolumnadapter.h"
#include "transactiontablemodel.h"
#include "ui/model/formats.h"
#include "ui/validation/detailvalidator.h"

#include <QDate>

// TransactionTypeColumnAdapter //

TransactionTypeColumnAdapter::TransactionTypeColumnAdapter(const QString &title, DataStore *dataStore)
    : ColumnAdapter(title, &TransactionDetail::categoryId, true)
    , dataStore{dataStore}
{}

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
    if (value.isValid()) {
        auto typeId = static_cast<const TransactionType*>(value.value<const NamedEntity*>());
        if (typeId->transfer) {
            row->transferAccountId = typeId->id;
            row->categoryId = QVariant{};
        } else {
            row->categoryId = typeId->id;
            row->transferAccountId = QVariant{};
        }
    } else {
        row->categoryId = QVariant{};
        row->transferAccountId = QVariant{};
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

SharesColumnAdapter::SharesColumnAdapter(const QString &title, const TransactionTableModel *model, const SecurityStore *securityStore)
    : AmountColumnAdapter{title, &TransactionDetail::assetQuantity, securityShares, true,
        new SharesValidatorFactory(model->payeeColumn, model->securityColumn, model->subtotalColumn)}
    , securityStore{securityStore}
    , dateColumn{model->dateColumn}
    , securityColumn{model->securityColumn}
{}

QVariant SharesColumnAdapter::value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const {
    if (index.isValid() && role == finances::AltDisplayRole) {
        auto shares = row->assetQuantity;
        if (!shares.isNull()) {
            auto securityId = index.parent().siblingAtColumn(securityColumn).data(finances::EntityIdRole);
            auto date = index.parent().siblingAtColumn(dateColumn).data(Qt::EditRole);
            auto shares = AmountColumnAdapter::value(row, index, current, Qt::EditRole).value<QDecNumber>();
            auto adjustedShares = securityStore->adjustedShares(securityId, date.value<QDate>(), shares);
            if (shares != adjustedShares) return formatter(QVariant::fromValue(adjustedShares));
        }
        return QVariant{};
    }
    return AmountColumnAdapter::value(row, index, current, role);
}

// DetailAmountColumnAdapter //

DetailAmountColumnAdapter::DetailAmountColumnAdapter(const QString &title)
    : AmountColumnAdapter{title, &TransactionDetail::amount, dollarFormat, true, new DetailAmountValidatorFactory()}
{}

// EmptyColumnAdapter //

EmptyColumnAdapter::EmptyColumnAdapter() : ColumnAdapter{"", nullptr, false} {}

QVariant EmptyColumnAdapter::value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const {
    return QVariant{};
}
