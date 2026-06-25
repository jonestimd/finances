#include "detailcolumnadapter.h"
#include "transactiontablemodel.h"
#include "ui/model/formats.h"
#include "ui/validation/detailvalidator.h"

#include <QDate>

// TransactionTypeColumnAdapter //

TransactionTypeColumnAdapter::TransactionTypeColumnAdapter(const QString &title, DataStore *dataStore, qlonglong accountId)
    : ColumnAdapter(title, true)
    , dataStore{dataStore}
    , accountId{accountId}
{}

QVariant TransactionTypeColumnAdapter::value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const {
    QVariant value = ColumnAdapter::value(row, index, getId(current), Qt::DisplayRole);
    const auto typeId = value.value<TransactionTypeId>();
    switch (role) {
    case Qt::DisplayRole:
        if (current.isValid() && current.isNull()) return "";
        if (typeId.id.has_value()) {
            if (typeId.transfer) return dataStore->accountStore->qualifiedName(typeId.id.value(), ':'); // .prepend("\u279c ");
            return dataStore->categoryStore->displayName(typeId.id.value());
        }
        break;
    case Qt::EditRole:
        if (current.isValid()) return current;
        if (value.isValid() && !value.isNull()) {
            if (!typeId.id.has_value()) return QVariant::fromValue<const NamedEntity*>(nullptr);
            if (typeId.transfer) return QVariant::fromValue<const NamedEntity*>(dataStore->accountStore->value(typeId.id.value()));
            return QVariant::fromValue<const NamedEntity*>(dataStore->categoryStore->value(typeId.id.value()));
        }
        break;
    case finances::OptionsRole:
        return QVariant::fromValue(getOptions());
    case Qt::DecorationRole:
        return value.isValid() && typeId.transfer ? finances::ArrowRight : finances::None;
    }
    return QVariant{};
}

QVariant TransactionTypeColumnAdapter::rowValue(const TransactionDetail *row) const {
    if (row->transferAccountId.has_value()) {
        return QVariant::fromValue(TransactionTypeId(true, row->transferAccountId));
    }
    return QVariant::fromValue(TransactionTypeId(false, row->categoryId));
}

void TransactionTypeColumnAdapter::setValue(TransactionDetail *row, QVariant value) const {
    if (value.value<const NamedEntity*>()) {
        auto typeId = static_cast<const TransactionType*>(value.value<const NamedEntity*>());
        if (typeId->transfer) {
            row->transferAccountId = typeId->id.value();
            row->categoryId = QVariant{};
        } else {
            row->categoryId = typeId->id.value();
            row->transferAccountId = {};
        }
    } else {
        row->categoryId = QVariant{};
        row->transferAccountId = {};;
    }
}

QVariant TransactionTypeColumnAdapter::getId(const QVariant &value) const {
    auto tt = static_cast<const TransactionType*>(value.value<const NamedEntity*>());
    return tt ? QVariant::fromValue(TransactionTypeId(tt)) : QVariant{};
}

QString TransactionTypeColumnAdapter::optionText(const NamedEntity* option) const {
    auto type = static_cast<const TransactionType*>(option);
    if (type->transfer) return dataStore->accountStore->qualifiedName(option->id.value(), ':');
    return dataStore->categoryStore->displayName(option->id.value());
}

ComboBoxModel *TransactionTypeColumnAdapter::getOptions() const {
    QList<const NamedEntity*> options;
    dataStore->categoryStore->appendValues(options);
    dataStore->accountStore->appendValues(options, {accountId});
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

QVariant DetailAmountColumnAdapter::value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const {
    return AmountColumnAdapter::value(row, index, current, role);
}

// EmptyColumnAdapter //

EmptyColumnAdapter::EmptyColumnAdapter() : ColumnAdapter{"", false} {}

QVariant EmptyColumnAdapter::rowValue(const TransactionDetail *row) const {
    return QVariant{};
}
