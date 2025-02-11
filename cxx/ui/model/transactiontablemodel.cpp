#include "transactiontablemodel.h"
#include "formatcolumnadapter.h"
#include "relationcolumnadapter.h"
#include "transactionstore.h"
#include "amountcolumnadapter.h"
#include "service/model/payee.h"
#include "formats.h"
#include <QDate>

#define PAYEE_TITLE "Payee"
#define SECURITY_TITLE "Security"
#define CLEARED_TITLE "🮱"
#define SUBTOTAL_TITLE "Subtotal"

#define INTERNAL_TX_ID(storeId) (storeId << 2 | 1)
#define INTERNAL_DETAIL_ID(storeId) (storeId << 2)
#define IS_TX_ID(internalId) (internalId & 1)
#define STORE_ID(internalId) ((qlonglong)(internalId >> 2))

namespace transactiontablemodel {
    QString txAmountFormat(const Transaction *row, const QVariant &amount) {
        return moneyFormat(amount).prepend("$"); // TODO currency symbol for account
    }

    class TxAmountColumnAdapter : public AmountColumnAdapter<Transaction> {
        TransactionStore *const store;

    public:
        TxAmountColumnAdapter(QString title, TransactionStore *store)
            : AmountColumnAdapter(title, &Transaction::id, dollarFormat, false)
            , store{store}
        {}

        virtual QVariant fieldValue(const Transaction *row) const override {
            return QVariant::fromValue(store->amount(row->id));
        }
    };

    class TxDateColumnAdapter : public FormatColumnAdapter<Transaction> {
    public:
        TxDateColumnAdapter(const QString &title) : FormatColumnAdapter{title, &Transaction::date, dateFormat, false} {}

        QVariant value(const Transaction *row, const QModelIndex &index, const QVariant current, int role) const override {
            if (role == finances::SortRole) {
                return QString("%1:%2").arg(row->date.toDate().toString(Qt::ISODate)).arg(row->id.toLongLong(), 16, 16);
            }
            return FormatColumnAdapter::value(row, index, current, role);
        }
    };

    class BalanceColumnAdapter : public AmountColumnAdapter<Transaction> {
        const TransactionTableModel *model;

    public:
        BalanceColumnAdapter(const QString &title, const TransactionTableModel *model)
            : AmountColumnAdapter{title, &Transaction::id, dollarFormat, false}
            , model{model}
        {}

    public:
        QVariant value(const Transaction *row, const QModelIndex &index, const QVariant current, int role) const override {
            if (!index.parent().isValid()) {
                switch (role) {
                case Qt::DisplayRole:
                case Qt::EditRole:
                case Qt::TextAlignmentRole:
                case finances::SortRole:
                case finances::TextHighlightRole:
                    return AmountColumnAdapter::value(row, index, model->balance(row->id), role);
                }
            }
            return QVariant{};
        }
    };

    class TransactionTypeColumnAdapter : public ColumnAdapter<TransactionDetail> {
        DataStore *const dataStore;
        // TransactionTableModel *const model;

    public:
        TransactionTypeColumnAdapter(const QString &title, DataStore *dataStore)
            : ColumnAdapter(title, &TransactionDetail::categoryId, false)
            , dataStore{dataStore}
        {}

        QVariant value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const override {
            // value is TransactionTypeId
            QVariant value = ColumnAdapter::value(row, index, getId(current), Qt::DisplayRole);
            switch (role) {
            case Qt::DisplayRole:
                if (current.isValid() && current.isNull()) return "";
                if (value.isValid() && !value.isNull()) {
                    const auto typeId = value.value<TransactionTypeId>();
                    if (typeId.transfer) return dataStore->accountStore->qualifiedName(typeId.id, ':'); // .prepend("\u279c ");
                    if (typeId.id.isValid()) return dataStore->categoryStore->displayName(typeId.id.toLongLong());
                }
                break;
            case Qt::EditRole: // TODO
                break;
            case finances::OptionsRole: // TODO
                break;
            case Qt::DecorationRole:
                if (value.isValid() && value.value<TransactionTypeId>().transfer) return finances::ArrowRight;
                return finances::None;
            }
            return QVariant{};
        }

        QVariant fieldValue(const TransactionDetail *row) const override {
            if (!row->relatedDetailId.isNull()) {
                auto rd = dataStore->transactionStore->detailStore.value(row->relatedDetailId);
                auto rx = dataStore->transactionStore->value(rd->transactionId);
                return QVariant::fromValue(TransactionTypeId(true, rx->accountId));
            }
            return QVariant::fromValue(TransactionTypeId(false, ColumnAdapter::fieldValue(row)));
        }

        void setValue(TransactionDetail *row, QVariant value) const override {
            auto typeId = value.value<TransactionTypeId>();
            if (typeId.transfer) {
                // TODO create related detail and tx?
            }
            else if (typeId.id.isValid()) ColumnAdapter::setValue(row, typeId.id);
        }

    private:
        QVariant getId(const QVariant &row) const {
            auto tt = row.value<const TransactionType*>();
            return tt ? QVariant::fromValue(TransactionTypeId(tt)) : QVariant{};
        }
    };

    class EmptyColumnAdapter : public ColumnAdapter<TransactionDetail> {
    public:
        EmptyColumnAdapter() : ColumnAdapter{"", nullptr, false} {}
        QVariant value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const override {
            return QVariant{};
        };
    };
}

using namespace transactiontablemodel;

TransactionTableModel::TransactionTableModel(DataStore *dataStore, qlonglong accountId)
    : PodItemModel{{
        new TxDateColumnAdapter{tr("Date")},
        new ColumnAdapter<Transaction>(tr("Ref #"), &Transaction::referenceNumber),
        new RelationColumnAdapter<Transaction, Payee, PayeeStore>(tr(PAYEE_TITLE), &Transaction::payeeId, dataStore->payeeStore),
        new ColumnAdapter<Transaction>(tr("Description"), &Transaction::memo),
        new RelationColumnAdapter<Transaction, Security, SecurityStore>(tr(SECURITY_TITLE), &Transaction::securityId, dataStore->securityStore),
        new TxAmountColumnAdapter(tr(SUBTOTAL_TITLE), dataStore->transactionStore),
        new ColumnAdapter<Transaction>(tr(CLEARED_TITLE), &Transaction::cleared),
        new BalanceColumnAdapter(tr("Balance"), this),
    }}
    , detailColumns{
        new EmptyColumnAdapter(), // TODO notification icons (missing lots)
        new RelationColumnAdapter<TransactionDetail, TransactionGroup, GroupStore>(tr("Group"), &TransactionDetail::groupId, dataStore->groupStore),
        new TransactionTypeColumnAdapter(tr("Category"), dataStore),
        new ColumnAdapter<TransactionDetail>(tr("Memo"), &TransactionDetail::memo),
        new AmountColumnAdapter<TransactionDetail>(tr("Shares"), &TransactionDetail::assetQuantity, securityShares, true),
        new AmountColumnAdapter<TransactionDetail>(tr("Amount"), &TransactionDetail::amount, dollarFormat, true),
        new EmptyColumnAdapter(),
        new EmptyColumnAdapter(),
    }
    , store{dataStore->transactionStore}
    , payeeColumn{columnIndex(tr(PAYEE_TITLE))}
    , securityColumn{columnIndex(tr(SECURITY_TITLE))}
    , clearedColumn{columnIndex(tr(CLEARED_TITLE))}
    , subtotalColumn{columnIndex(tr(SUBTOTAL_TITLE))}
    , accountId{accountId}
{
    connect(store, SIGNAL(accountLoaded(qlonglong)), this, SLOT(accountLoaded(qlonglong)));
}

TransactionTableModel::~TransactionTableModel() {
    qDeleteAll(detailColumns);
}

QList<qlonglong> TransactionTableModel::transactionIds() const {
    return store->transactionIds(accountId);
}

int TransactionTableModel::childCount(const QModelIndex &parent) const {
    return parent.isValid() ? getRow(parent)->detailIds.length() : transactionIds().count();
}

void TransactionTableModel::setRows(const QList<qlonglong> transactionIds) {
    beginResetModel();
    clearChanges();
    balances.clear();
    clearedBalance_ = 0;
    QDecNumber balance{0};
    for (auto id : transactionIds) {
        auto amount = store->amount(id);
        balance += amount;
        balances.insert(id, QVariant::fromValue(balance));
        if (store->value(id)->cleared.toBool()) clearedBalance_ += amount;
    }
    endResetModel();
    emit clearedBalanceChanged(clearedBalance_);
}

QVariant TransactionTableModel::balance(const QVariant &transactionId) const {
    return balances.value(transactionId.toLongLong());
}

QDecNumber TransactionTableModel::clearedBalance() const {
    return clearedBalance_;
}

QModelIndex TransactionTableModel::index(int row, int column, const QModelIndex &parent) const {
    if (hasIndex(row, column, parent)) {
        if (parent.isValid()) {
            auto txId = STORE_ID(parent.internalId());
            auto tx = store->value(txId);
            auto detailId = tx->detailIds.at(row).toLongLong();
            return createIndex(row, column, INTERNAL_DETAIL_ID(detailId));
        }
        auto txId = transactionIds().at(row);
        return createIndex(row, column, INTERNAL_TX_ID(txId));
    }
    return QModelIndex{};
}

QModelIndex TransactionTableModel::parent(const QModelIndex &child) const {
    if (child.isValid() && !IS_TX_ID(child.internalId())) {
        auto detail = store->detailStore.value(STORE_ID(child.internalId()));
        auto txId = detail->transactionId.toLongLong();
        auto row = transactionIds().indexOf(txId);
        return createIndex(row, 0, INTERNAL_TX_ID(txId));
    }
    return QModelIndex{};
}

int TransactionTableModel::rowCount(const QModelIndex &parent) const {
    if (parent.parent().isValid()) return 0;
    if (parent.isValid()) return childCount(parent);
    return transactionIds().count();
}

const Transaction *TransactionTableModel::getRow(const QModelIndex &index) const {
    auto id = transactionIds().at(index.row());
    return store->value(id);
}

QVariant TransactionTableModel::data(const QModelIndex &index, int role) const {
    if (index.parent().isValid()) {
        auto detailId = getRow(index.parent())->detailIds.at(index.row());
        auto detail = store->detailStore.value(detailId.toLongLong());
        auto value = detailColumns[index.column()]->value(detail, index, QVariant{}, role);
        if (role == finances::TextHighlightRole) {
            return value.value<finances::TextHighlight>() + finances::Dimmed;
        }
        return value;
    }
    if (role == Qt::FontRole && isBoldColumn(index.column())) return boldFont();
    return PodItemModel::data(index, role);
}

bool TransactionTableModel::setData(const QModelIndex &index, const QVariant &value, int role) {
    if (index.parent().isValid()) {
        if (role == Qt::EditRole && index.column() == subtotalColumn) {
            if (data(index.parent().siblingAtColumn(clearedColumn), Qt::DisplayRole).toBool()) {
                auto oldValue = data(index, Qt::DisplayRole);
                if (value != oldValue) {
                    // TODO update cleared balance
                }
            }
        }
        return false;
    }
    if (role == Qt::EditRole && index.column() == clearedColumn) {
        if (value != data(index, Qt::DisplayRole)) {
            auto amount = data(index.siblingAtColumn(subtotalColumn), Qt::EditRole).value<QDecNumber>();
            if (value.toBool()) clearedBalance_ += amount;
            else clearedBalance_ -= amount;
            emit clearedBalanceChanged(clearedBalance_);
        }
    }
    return PodItemModel::setData(index, value, role);
}

QVariant TransactionTableModel::headerData(int section, Qt::Orientation orientation, int role) const {
    if (section == clearedColumn && role == Qt::TextAlignmentRole) return Qt::AlignCenter;
    auto value = PodItemModel::headerData(section, orientation, role);
    if (orientation == Qt::Orientation::Horizontal && role == Qt::DisplayRole) {
        return value.toString().append('\n').append(detailColumns.at(section)->title);
    }
    return value;
}

void TransactionTableModel::accountLoaded(qlonglong accountId) {
    if (accountId == this->accountId) setRows(store->transactionIds(accountId));
}

bool TransactionTableModel::isBoldColumn(int column) const {
    return (column == payeeColumn || column == securityColumn || column == subtotalColumn);
}

QFont TransactionTableModel::boldFont() {
    QFont font(qApp->font());
    font.setBold(true);
    return font;
}
