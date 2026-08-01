#include "accountsecuritymodel.h"
#include "amountcolumnadapter.h"
#include "formatcolumnadapter.h"
#include "formats.h"
#include "numbercolumnadapter.h"
#include "ui/store/datastore.h"

#define CHILD_INDEX_ID(parent) (parent.row() + 1)
#define PARENT_INDEX_ID (quintptr(0))
#define HAS_PARENT_ROW(child) (child.internalId())
#define PARENT_ROW(child) (child.internalId() - 1)

namespace accountsecuritymodel {
    template<class Value>
    class SecurityColumnAdapter : public ColumnAdapter<AccountSecurity> {
        const SecurityStore* store;
        Value Security::* field;
    public:
        SecurityColumnAdapter(const SecurityStore* store, const QString title, Value Security::* field)
            : ColumnAdapter{title, false}
            , store{store}
            , field{field}
        {}

        virtual QVariant rowValue(const AccountSecurity* row) const override {
            return store->value(row->id.securityId)->*field;
        }
    };

    class SecurityTypeColumnAdapter : public ColumnAdapter<AccountSecurity> {
        const SecurityStore* store;
    public:
        SecurityTypeColumnAdapter(const SecurityStore* store)
            : ColumnAdapter{QObject::tr("Type"), false}
            , store{store}
        {}

        virtual QVariant rowValue(const AccountSecurity* row) const override {
            return store->value(row->id.securityId)->securityType->name;
        }
    };
}

using namespace accountsecuritymodel;

AccountSecurityTableModel::AccountSecurityTableModel(DataStore* dataStore)
    : QAbstractItemModel{}
    , accountStore{dataStore->accountStore}
    , securityStore{dataStore->securityStore}
    , columns{
        new SecurityColumnAdapter<QString>(dataStore->securityStore, tr("Security"), &Security::name),
        new SecurityTypeColumnAdapter(dataStore->securityStore),
        new SecurityColumnAdapter<QString>(dataStore->securityStore, tr("Symbol"), &Security::symbol),
        new FormatColumnAdapter<AccountSecurity, std::optional<QDate>>(tr("First Acquired"), &AccountSecurity::firstAcquired, dateFormat, false),
        new NumberColumnAdapter<AccountSecurity, int>(tr("Transactions"), &AccountSecurity::transactions),
        new AmountColumnAdapter<AccountSecurity, QDecNumber>(tr("Shares"), &AccountSecurity::shares, securityShares, false),
        new AmountColumnAdapter<AccountSecurity, QDecNumber>(tr("Cost Basis"), &AccountSecurity::costBasis, dollarFormat, false),
    }
{}

AccountSecurityTableModel::~AccountSecurityTableModel() {
    qDeleteAll(columns);
    reset();
}

void AccountSecurityTableModel::reset() {
    accountIds.clear();
    for (auto i = byAccount.constBegin(); i != byAccount.constEnd(); i++) qDeleteAll(i.value());
    byAccount.clear();
}

QVariant AccountSecurityTableModel::headerData(int section, Qt::Orientation orientation, int role) const {
    if (role == Qt::DisplayRole && orientation == Qt::Horizontal && section >= 0 && section < columns.size()) {
        return columns[section]->title;
    }
    return QVariant{};
}

QModelIndex AccountSecurityTableModel::index(int row, int column, const QModelIndex &parent) const {
    if (hasIndex(row, column, parent)) {
        if (parent.isValid()) return createIndex(row, column, CHILD_INDEX_ID(parent));
        return createIndex(row, column, PARENT_INDEX_ID);
    }
    return QModelIndex{};
}

QModelIndex AccountSecurityTableModel::parent(const QModelIndex &child) const {
    if (child.isValid() && HAS_PARENT_ROW(child)) return createIndex(PARENT_ROW(child), 0, quintptr(0));
    return QModelIndex{};
}

int AccountSecurityTableModel::rowCount(const QModelIndex &parent) const {
    if (parent.parent().isValid()) return 0;
    if (parent.isValid()) {
        auto accountId = accountIds.at(parent.row());
        return byAccount.value(accountId).size();
    }
    return accountIds.size();
}

int AccountSecurityTableModel::columnCount(const QModelIndex &parent) const {
    return columns.size();
}

QVariant AccountSecurityTableModel::data(const QModelIndex &index, int role) const {
    if (index.parent().isValid()) {
        auto accountId = accountIds.at(index.parent().row());
        auto accountSecurity = byAccount.value(accountId).at(index.row());
        return columns.at(index.column())->value(accountSecurity, index, {}, role);
    }
    if (index.column() == 0) {
        switch (role) {
        case Qt::DisplayRole:
        case finances::SortRole:;
            return accountStore->value(accountIds.at(index.row()))->name;
        case Qt::FontRole:
            return finances::boldFont();
        }
    }
    return QVariant{};
}

void AccountSecurityTableModel::setRows(QList<const AccountSecurity*> rows) {
    beginResetModel();
    reset();
    for (const auto row: rows) {
        auto accountId = row->id.accountId;
        if (!accountIds.contains(accountId)) {
            accountIds.append(accountId);
            byAccount.insert(accountId, {});
        }
        byAccount[accountId].append(row);
    }
    endResetModel();
}

void AccountSecurityTableModel::updateRows(QList<const AccountSecurity*> rows) {
    for (const auto row : rows) {
        auto accountId = row->id.accountId;
        auto rowIndex = indexOf(row->id);
        if (rowIndex.isValid()) {
            auto& summaries = byAccount[accountId];
            if (rowIndex.parent().isValid()) {
                summaries[rowIndex.row()] = row;
                emit dataChanged(rowIndex, rowIndex.siblingAtColumn(columns.size()-1));
            } else {
                beginInsertRows(rowIndex, summaries.size(), summaries.size());
                summaries.append(row);
                endInsertRows();
            }
        } else {
            auto parent = index(accountIds.size(), 0);
            beginInsertRows(parent, 0, 0);
            accountIds.append(accountId);
            byAccount.insert(accountId, {row});
            endInsertRows();
        }
    }
}

void AccountSecurityTableModel::removeRows(const QList<AccountSecurityId> ids) {
    for (auto id : ids) {
        auto rowIndex = indexOf(id);
        if (rowIndex.parent().isValid()) {
            auto& summaries = byAccount[id.accountId];
            if (summaries.size() > 1) {
                beginRemoveRows(rowIndex.parent(), rowIndex.row(), rowIndex.row());
                summaries.removeAt(rowIndex.row());
                endRemoveRows();
            } else {
                beginRemoveRows({}, rowIndex.parent().row(), rowIndex.parent().row());
                accountIds.removeOne(id.accountId);
                byAccount.remove(id.accountId);
                qDeleteAll(summaries);
                endRemoveRows();
            }
        }
    }
}

QModelIndex AccountSecurityTableModel::indexOf(AccountSecurityId id) {
    if (byAccount.contains(id.accountId)) {
        auto parent = index(accountIds.indexOf(id.accountId), 0, {});
        auto summaries = byAccount[id.accountId];
        for (auto i = summaries.cbegin(); i != summaries.cend(); i++) {
            if ((*i)->id.securityId == id.securityId) return index(i - summaries.cbegin(), 0, parent);
        }
        return parent;
    }
    return {};
}
