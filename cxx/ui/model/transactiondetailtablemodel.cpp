#include "transactiondetailtablemodel.h"
#include "ui/store/datastore.h"
#include "ui/model/detailcolumnadapter.h"
#include "ui/model/formats.h"
#include "ui/model/formatcolumnadapter.h"
#include "ui/model/relationcolumnadapter.h"
#include "ui/titles.h"

const int TransactionDetailTableModel::dateColumn{0};
const int TransactionDetailTableModel::securityColumn{4};
const int TransactionDetailTableModel::sharesColumn{8};

TransactionDetailTableModel::TransactionDetailTableModel(DataStore* dataStore)
    : QAbstractTableModel{}
    , dataStore{dataStore}
    , txColumns{
        new FormatColumnAdapter{tr(DATE_TITLE), &SearchTransactionDetail::transactionDate, dateFormat, false},
        new RelationColumnAdapter<SearchTransactionDetail, Account, AccountStore, domain_id>(tr(ACCOUNT_TITLE), &SearchTransactionDetail::accountId, dataStore->accountStore),
        new RelationColumnAdapter<SearchTransactionDetail, Payee, PayeeStore, optional_id>(tr(PAYEE_TITLE), &SearchTransactionDetail::payeeId, dataStore->payeeStore),
        new FieldColumnAdapter<SearchTransactionDetail, QString>{tr(TX_MEMO_TITLE), &SearchTransactionDetail::transactionMemo},
        new RelationColumnAdapter<SearchTransactionDetail, Security, SecurityStore, optional_id>(tr(SECURITY_TITLE), &SearchTransactionDetail::securityId, dataStore->securityStore),
    }
    , detailColumns{
        new TransactionTypeColumnAdapter(tr(CATEGORY_TITLE), dataStore, {}),
        new RelationColumnAdapter<TransactionDetail, TransactionGroup, GroupStore, optional_id>(tr(GROUP_TITLE), &SearchTransactionDetail::groupId, dataStore->groupStore),
        new FieldColumnAdapter<TransactionDetail, QString>(tr(DETAIL_MEMO_TITLE), &TransactionDetail::memo),
        new SharesColumnAdapter(tr(SHARES_TITLE), this, dataStore->securityStore),
        new DetailAmountColumnAdapter(tr(AMOUNT_TITLE)),
    }
{
    connect(&dataStore->securityStore->stockSplitStore, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(splitsLoaded()));
}

TransactionDetailTableModel::~TransactionDetailTableModel() {
    qDeleteAll(txColumns);
    qDeleteAll(detailColumns);
    reset();
}

QVariant TransactionDetailTableModel::headerData(int section, Qt::Orientation orientation, int role) const {
    if (role == Qt::DisplayRole && orientation == Qt::Horizontal) {
        if (section >= 0 && section < txColumns.size()) return txColumns[section]->title;
        if (section >= txColumns.size() && section < txColumns.size() + detailColumns.size()) {
            return detailColumns[section-txColumns.size()]->title;
        }
    }
    return QVariant{};
}

int TransactionDetailTableModel::columnCount(const QModelIndex& parent) const {
    return parent.isValid() ? 0 : txColumns.size() + detailColumns.size();
}

int TransactionDetailTableModel::rowCount(const QModelIndex& parent) const {
    if (parent.isValid()) return 0;
    return details.size();
}

QVariant TransactionDetailTableModel::data(const QModelIndex& index, int role) const {
    if (!index.parent().isValid() && index.row() < details.size()) {
        auto detail = details.at(index.row());
        if (index.column() < txColumns.size()) {
            return txColumns.at(index.column())->value(detail, index, {}, role);
        }
        auto adapterIndex = index.column() - txColumns.size();
        if (adapterIndex < detailColumns.size()) {
            return detailColumns.at(adapterIndex)->value(detail, index, {}, role);
        }
    }
    return QVariant();
}

QVariant TransactionDetailTableModel::detailDate(const QModelIndex &index) {
    return index.siblingAtColumn(dateColumn).data(Qt::EditRole);
}

QVariant TransactionDetailTableModel::detailSecurityId(const QModelIndex &index) {
    return index.siblingAtColumn(securityColumn).data(finances::EntityIdRole);
}

void TransactionDetailTableModel::setRows(QList<const SearchTransactionDetail*> rows) {
    beginResetModel();
    reset();
    details.append(rows);
    endResetModel();
}

void TransactionDetailTableModel::splitsLoaded() {
    auto rows = rowCount({});
    if (rows) emit dataChanged(index(0, sharesColumn, {}), index(rows-1, sharesColumn, {}));
}

void TransactionDetailTableModel::reset() {
    qDeleteAll(details);
    details.clear();
}
