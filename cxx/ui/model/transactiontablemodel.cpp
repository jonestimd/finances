#include "transactiontablemodel.h"
#include "formatcolumnadapter.h"
#include "relationcolumnadapter.h"
#include "transactionstore.h"
#include "amountcolumnadapter.h"
#include "service/model/payee.h"
#include "formats.h"
#include <QDate>

#define DETAIL_ROW_TYPE 1

#define DATE_TITLE "Date"
#define PAYEE_TITLE "Payee"
#define SECURITY_TITLE "Security"
#define CLEARED_TITLE "🮱"
#define SUBTOTAL_TITLE "Subtotal"
#define BALANCE_TITLE "Balance"

#define CHILD_INDEX_ID(parent) (parent.row() + 1)
#define PARENT_INDEX_ID (quintptr(0))
#define HAS_TX_ROW(child) (child.internalId())
#define TX_ROW(child) (child.internalId() - 1)

namespace transactiontablemodel {
    QString txAmountFormat(const Transaction *row, const QVariant &amount) {
        return moneyFormat(amount).prepend("$"); // TODO currency symbol for account
    }

    class TxAmountColumnAdapter : public AmountColumnAdapter<Transaction> {
        TransactionTableModel *const model;

    public:
        TxAmountColumnAdapter(QString title, TransactionTableModel *model)
            : AmountColumnAdapter(title, &Transaction::id, dollarFormat, false)
            , model{model}
        {}

        QVariant value(const Transaction *row, const QModelIndex &index, const QVariant current, int role) const override {
            switch (role) {
            case Qt::DisplayRole:
            case Qt::EditRole:
            case finances::SortRole:
            case finances::TextHighlightRole:
                QDecNumber total{0};
                auto detailCount = model->rowCount(index);
                for (int i = 0; i < detailCount; i++) {
                    total += model->data(this->model->index(i, index.column(), index), Qt::EditRole).value<QDecNumber>();
                }
                return AmountColumnAdapter::value(row, index, QVariant::fromValue(total), role);
            }
            return AmountColumnAdapter::value(row, index, current, role);
        }
    };

    class TxDateColumnAdapter : public FormatColumnAdapter<Transaction> {
    public:
        TxDateColumnAdapter(const QString &title) : FormatColumnAdapter{title, &Transaction::date, dateFormat, true} {}

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
}

using namespace transactiontablemodel;

TransactionTableModel::TransactionTableModel(DataStore *dataStore, qlonglong accountId)
    : PodItemModel{{
        new TxDateColumnAdapter{tr(DATE_TITLE)},
        new ColumnAdapter<Transaction>(tr("Ref #"), &Transaction::referenceNumber),
        new RelationColumnAdapter<Transaction, Payee, PayeeStore>(tr(PAYEE_TITLE), &Transaction::payeeId, dataStore->payeeStore),
        new ColumnAdapter<Transaction>(tr("Description"), &Transaction::memo),
        new RelationColumnAdapter<Transaction, Security, SecurityStore>(tr(SECURITY_TITLE), &Transaction::securityId, dataStore->securityStore),
        new TxAmountColumnAdapter(tr(SUBTOTAL_TITLE), this),
        new ColumnAdapter<Transaction>(tr(CLEARED_TITLE), &Transaction::cleared),
        new BalanceColumnAdapter(tr(BALANCE_TITLE), this),
    }}
    , transactionTypeAdapter{new TransactionTypeColumnAdapter(tr("Category"), dataStore)}
    , detailColumns{
        new EmptyColumnAdapter(), // TODO notification icons (missing lots)
        new RelationColumnAdapter<TransactionDetail, TransactionGroup, GroupStore>(tr("Group"), &TransactionDetail::groupId, dataStore->groupStore),
        transactionTypeAdapter,
        new ColumnAdapter<TransactionDetail>(tr("Memo"), &TransactionDetail::memo),
        new SharesColumnAdapter(tr("Shares"), this, dataStore->securityStore),
        new DetailAmountColumnAdapter(tr("Amount")),
        new EmptyColumnAdapter(),
        new EmptyColumnAdapter(),
    }
    , store{dataStore->transactionStore}
    , dateColumn{columnIndex(tr(DATE_TITLE))}
    , payeeColumn{columnIndex(tr(PAYEE_TITLE))}
    , securityColumn{columnIndex(tr(SECURITY_TITLE))}
    , clearedColumn{columnIndex(tr(CLEARED_TITLE))}
    , subtotalColumn{columnIndex(tr(SUBTOTAL_TITLE))}
    , balanceColumn{columnIndex(tr(BALANCE_TITLE))}
    , accountId{accountId}
{
    connect(store, SIGNAL(accountLoaded(qlonglong)), this, SLOT(accountLoaded(qlonglong)));
    connect(dataStore->categoryStore, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(updateBalances()));
}

TransactionTableModel::~TransactionTableModel() {
    qDeleteAll(detailColumns);
}

const QList<qlonglong> TransactionTableModel::transactionIds() const {
    return store->transactionIds(accountId);
}

QVariant TransactionTableModel::value(const QModelIndex &index, int role, QVariant current = QVariant{}) const {
    if (index.parent().isValid()) {
        return detailColumns.at(index.column())->value(getDetail(index), index, current, role);
    }
    return PodItemModel::value(index, role, current);
}

void TransactionTableModel::setValue(const QModelIndex &index, const QVariant &value) {
    if (index.parent().isValid()) {
        auto detail = pendingDetails(index.parent()).at(index.row() - childCount(index.parent()));
        setDetailValue(detail, index.column(), value);
    }
    else PodItemModel::setValue(index, value);
}

void TransactionTableModel::setDetailValue(TransactionDetail *detail, int columnIndex, const QVariant &value) {
    auto column = detailColumns.at(columnIndex);
    if (column == transactionTypeAdapter) transactionTypeAdapter->setValue(detail, value);
    else column->setValue(detail, value);
}

int TransactionTableModel::childCount(const QModelIndex &parent) const {
    return parent.isValid() ? getRow(parent)->detailIds.length() : transactionIds().count();
}

const QList<TransactionDetail*> TransactionTableModel::pendingDetails(const QModelIndex &parent) const {
    return newDetails.value(parent.row(), QList<TransactionDetail*>());
}

void TransactionTableModel::updateBalances(int fromRow, const QDecNumber &delta) {
    const auto ids = transactionIds().sliced(fromRow);
    for (auto id : ids) {
        auto balance = balances.value(id).value<QDecNumber>();
        balances.insert(id, QVariant::fromValue(balance + delta));
    }
    emit dataChanged(index(fromRow, balanceColumn), index(rowCount()-1, balanceColumn));
}

void TransactionTableModel::updateBalances() {
    balances.clear();
    clearedBalance_ = 0;
    QDecNumber balance{0};
    for (auto id : transactionIds()) {
        auto amount = store->amount(id);
        balance += amount;
        balances.insert(id, QVariant::fromValue(balance));
        if (store->value(id)->cleared.toBool()) clearedBalance_ += amount;
    }
    emit dataChanged(index(0, balanceColumn), index(rowCount()-1, balanceColumn));
    emit clearedBalanceChanged(clearedBalance_);
}

void TransactionTableModel::updateClearedBalance(const QDecNumber &delta) {
    clearedBalance_ += delta;
    emit clearedBalanceChanged(clearedBalance_);
}

Transaction *TransactionTableModel::newRow() {
    return new Transaction(accountId);
}

void TransactionTableModel::setRows(const QList<qlonglong> transactionIds) {
    beginResetModel();
    clearChanges();
    updateBalances();
    endResetModel();
    queueAdd(QModelIndex{});
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
        if (parent.isValid()) return createIndex(row, column, CHILD_INDEX_ID(parent));
        return createIndex(row, column, PARENT_INDEX_ID);
    }
    return QModelIndex{};
}

QModelIndex TransactionTableModel::parent(const QModelIndex &child) const {
    if (child.isValid() && HAS_TX_ROW(child)) return createIndex(TX_ROW(child), 0, quintptr(0));
    return QModelIndex{};
}

int TransactionTableModel::rowCount(const QModelIndex &parent) const {
    if (parent.parent().isValid()) return 0;
    if (parent.isValid()) return childCount(parent) + pendingDetails(parent).length();
    return transactionIds().count() + newRows.value(parent).length();
}

int TransactionTableModel::rowType(const QModelIndex &index) const {
    return index.parent().isValid() ? DETAIL_ROW_TYPE : PodItemModel::rowType(index);
}

const Transaction *TransactionTableModel::getRow(const QModelIndex &index) const {
    if (index.row() < transactionIds().size()) {
        auto id = transactionIds().at(index.row());
        return store->value(id);
    }
    return newRows.value(QModelIndex{}).at(index.row() - transactionIds().size());
}

const TransactionDetail *TransactionTableModel::getDetail(const QModelIndex &index) const {
    auto txDetailIds = getRow(index.parent())->detailIds;
    if (index.row() < txDetailIds.length()) {
        return store->detailStore.value(txDetailIds.at(index.row()));
    }
    return newDetails[index.parent().row()].at(index.row()-txDetailIds.length());
}

AbstractColumnAdapter *TransactionTableModel::adapter(const QModelIndex &index) const {
    if (index.parent().isValid()) return detailColumns.at(index.column());
    return PodItemModel::adapter(index);
}

bool TransactionTableModel::isPendingDelete(const QModelIndex &index) const {
    return PodItemModel::isPendingDelete(index) || index.parent().isValid() && isPendingDelete(index.parent());
}

QVariant TransactionTableModel::data(const QModelIndex &index, int role) const {
    if (index.parent().isValid()) {
        switch (role) {
        case finances::TextHighlightRole:
            auto value = this->value(index, role, changes.value(index));
            return value.value<finances::TextHighlight>() + finances::Dimmed;
        }
    }
    else if (role == Qt::FontRole && isBoldColumn(index.column())) return boldFont();
    return PodItemModel::data(index, role);
}

bool TransactionTableModel::setData(const QModelIndex &index, const QVariant &value, int role) {
    if (role == Qt::EditRole) {
        if (index.parent().isValid()) {
            auto parsed = detailColumns.at(index.column())->parseValue(value);
            if (index.column() == subtotalColumn) {
                auto delta = parsed.value<QDecNumber>() - data(index, Qt::EditRole).value<QDecNumber>();
                updateBalances(index.parent().row(), delta);
                if (data(index.parent().siblingAtColumn(clearedColumn)).toBool()) updateClearedBalance(delta);
                auto changeIndex = index.parent().siblingAtColumn(subtotalColumn);
                emit dataChanged(changeIndex, changeIndex);
            }
        } else if (index.column() == clearedColumn) {
            if (value != data(index, Qt::DisplayRole)) {
                auto amount = data(index.siblingAtColumn(subtotalColumn), Qt::EditRole).value<QDecNumber>();
                updateClearedBalance(value.toBool() ? amount : amount.minus());
            }
        }
    }
    return PodItemModel::setData(index, value, role);
}

Qt::ItemFlags TransactionTableModel::flags(const QModelIndex &index) const {
    if (index.parent().isValid()) {
        bool pendingDelete = pendingDeletes.contains(index.siblingAtColumn(0));
        return AdapterItemModel::flags(index) | detailColumns[index.column()]->flags(getDetail(index), !pendingDelete);
    }
    return PodItemModel::flags(index);
}

QVariant TransactionTableModel::headerData(int section, Qt::Orientation orientation, int role) const {
    if (section == clearedColumn && role == Qt::TextAlignmentRole) return Qt::AlignCenter;
    auto value = PodItemModel::headerData(section, orientation, role);
    if (orientation == Qt::Horizontal && role == Qt::DisplayRole) {
        return value.toString().append('\n').append(detailColumns.at(section)->title);
    }
    return value;
}

bool TransactionTableModel::hasUnsavedChanges() const {
    auto pendingTransactions = newRows.value(QModelIndex{});
    if (pendingTransactions.size() == 1) {
        auto tx = pendingTransactions.at(0);
        if (!tx->isEmpty()) return true;
        auto details = newDetails.value(rowCount() - 1);
        for (auto detail : std::as_const(details)) {
            if (!detail->isEmpty()) return true;
        }
        return false;
    }
    return !newDetails.isEmpty() || PodItemModel::hasUnsavedChanges();
}

void TransactionTableModel::clearChanges() {
    for (auto i = newDetails.begin(); i != newDetails.end(); i = newDetails.erase(i)) {
        auto parentIndex = createIndex(i.key(), 0);
        auto tx = getRow(parentIndex);
        auto count = tx->detailIds.size() + i.value().size();
        beginRemoveRows(parentIndex, tx->detailIds.size(), count-1);
        while (!i.value().isEmpty()) delete i.value().takeFirst();
        endRemoveRows();
    }
    PodItemModel::clearChanges();
}

bool TransactionTableModel::enableDelete(const QModelIndex &index) const {
    if (index.parent().isValid()) {
        if (isPendingAdd(index.parent())) {
            return newRows.value(QModelIndex{}).size() > 1 || newDetails.value(index.parent().row()).size() > 1;
        }
    } else if (isPendingAdd(index)) {
        return newRows.value(QModelIndex{}).size() > 1;
    }
    return PodItemModel::enableDelete(index);
}

bool TransactionTableModel::transactionHasChanges(const QModelIndex &rowIndex) const {
    auto txIndex = rowIndex.parent().isValid() ? rowIndex.parent() : rowIndex;
    auto txRow = txIndex.row();
    if (txRow >= transactionIds().size() || isPendingDelete(txIndex) || newDetails.contains(txRow)) return true;
    for (auto i = changes.keyBegin(); i != changes.keyEnd(); i++) {
        if (!i->parent().isValid() && i->row() == txRow || i->parent() == txIndex) return true;
    }
    auto detailCount = rowCount(txIndex);
    for (int i = 0; i < detailCount; i++) {
        if (isPendingDelete(index(i, 0, txIndex))) return true;
    }
    return false;
}

bool TransactionTableModel::transactionIsValid(const QModelIndex &rowIndex) const {
    auto txIndex = rowIndex.parent().isValid() ? rowIndex.parent() : rowIndex;
    auto txRow = txIndex.row();
    for (auto i = errors.keyBegin(); i != errors.keyEnd(); i++) {
        if (i->row() == txRow || i->parent() == txIndex) return false;
    }
    int detailCount = rowCount(txIndex), validDetails = 0;
    for (int i = 0; i < detailCount; i++) {
        auto detail = getDetail(index(i, 0, txIndex));
        if (!detail->isEmpty()) validDetails++;
    }
    return validDetails > 0;
}

QHash<const Transaction*, QList<TransactionDetail*>> TransactionTableModel::unsavedDetailAdds() const {
    QHash<const Transaction*, QList<TransactionDetail*>> adds{};
    for (auto [rowIndex, details] : newDetails.asKeyValueRange()) {
        auto tx = getRow(createIndex(rowIndex, 0));
        QList<TransactionDetail*> txDetails{};
        for (auto detail : details) txDetails.append(new TransactionDetail(*detail));
        adds.insert(tx, txDetails);
    }
    return adds;
}

QList<TransactionDetail*> TransactionTableModel::unsavedDetailChanges() { // TODO reuse code in PodItemModel
    QHash<const QList<int>, TransactionDetail*> changeRows;
    for (auto [index, value] : changes.asKeyValueRange()) {
        if (isPendingDelete(index) || rowType(index) != DETAIL_ROW_TYPE) continue;
        TransactionDetail *updated;
        auto indexes = rowIndexes(index);
        if (changeRows.contains(indexes)) updated = changeRows[indexes];
        else {
            updated = new TransactionDetail(*getDetail(index));
            changeRows[indexes] = updated;
        }
        setDetailValue(updated, index.column(), value);
    }
    return changeRows.values();
}

QList<const TransactionDetail*> TransactionTableModel::unsavedDetailDeletes() const {
    QList<const TransactionDetail*> deletes{};
    for (auto index : pendingDeletes) {
        if (index.parent().isValid()) {
            deletes.append(getDetail(index));
        }
    }
    return deletes;
}

void TransactionTableModel::accountLoaded(qlonglong accountId) {
    if (accountId == this->accountId) setRows(store->transactionIds(accountId));
}

int TransactionTableModel::pendingDeleteCount(const QModelIndex &parent) const {
    int count = 0;
    for (auto index : pendingDeletes) {
        if (index.parent().isValid() && index.parent().row() == parent.row()) count++;
    }
    return count;
}

bool TransactionTableModel::isBoldColumn(int column) const {
    return (column == payeeColumn || column == securityColumn || column == subtotalColumn);
}

QFont TransactionTableModel::boldFont() {
    QFont font(qApp->font());
    font.setBold(true);
    return font;
}

QModelIndex TransactionTableModel::queueAdd(const QModelIndex &selectedIndex) {
    auto parent = selectedIndex.parent().isValid() ? selectedIndex.parent() : selectedIndex;
    if (parent.isValid() && parent.model() == this) {
        auto rowIndex = rowCount(parent);
        beginInsertRows(parent, rowIndex, rowIndex);
        if (!newDetails.contains(parent.row())) newDetails.insert(parent.row(), QList<TransactionDetail*>());
        newDetails[parent.row()].append(new TransactionDetail(getRow(parent)->id));
        validateRow(rowIndex, parent);
        endInsertRows();
        return index(rowIndex, 0, parent);
    }
    auto index = PodItemModel::queueAdd(QModelIndex{});
    setValue(index, QDate::currentDate());
    beginInsertRows(index, 0, 0);
    newDetails.insert(index.row(), QList{new TransactionDetail});
    endInsertRows();
    return index;
}

void TransactionTableModel::queueDelete(const QModelIndex &index) {
    auto parent = index.parent();
    if (parent.isValid()) {
        if (isPendingAdd(index)) {
            auto indexRow = index.row();
            beginRemoveRows(parent, indexRow, indexRow);
            delete newDetails[parent.row()].takeAt(indexRow - childCount(parent));
            if (newDetails[parent.row()].isEmpty()) newDetails.remove(parent.row());
            endRemoveRows();
            adjustErrorIndexes(indexRow, parent, -1);
            removeStaleErrors();
            if (isPendingAdd(parent) && !newDetails.contains(parent.row())) {
                PodItemModel::queueDelete(parent);
            }
        } else if (!isPendingDelete(parent)) {
            if (rowCount(parent) - pendingDeleteCount(parent) == 1) {
                queueDelete(parent);
                const auto row = parent.row();
                pendingDeletes.removeIf([=](const QModelIndex &i) {
                    return i.parent().isValid() && i.parent().row() == row;
                });
            } else PodItemModel::queueDelete(index);
        }
    } else {
        if (isPendingAdd(index)) {
            auto details = newDetails.value(index.row());
            beginRemoveRows(index, 0, details.size()-1);
            while (!details.isEmpty()) delete details.takeLast();
            newDetails.remove(index.row());
            endRemoveRows();
        } else {
            pendingDeletes.removeIf([index](const QModelIndex &i) { return i.parent() == index; });
            rowsChanged(0, rowCount(index)-1, index);
        }
        PodItemModel::queueDelete(index);
    }
}

void TransactionTableModel::undoChange(const QModelIndex &index) {
    auto oldValue = index.data(Qt::EditRole);
    auto undoDelete = !index.parent().isValid() && isPendingDelete(index);
    PodItemModel::undoChange(index);
    if (index.parent().isValid() && index.column() == subtotalColumn) {
        auto parent = index.parent().siblingAtColumn(subtotalColumn);
        auto delta = index.data(Qt::EditRole).value<QDecNumber>() - oldValue.value<QDecNumber>();
        updateBalances(parent.row(), delta);
        if (parent.siblingAtColumn(clearedColumn).data().toBool()) updateClearedBalance(delta);
        emit dataChanged(parent, parent, QList<int>{Qt::DisplayRole});
    }
    if (undoDelete) rowsChanged(0, rowCount(index)-1, index);
}
