#include "accountsecuritymodel.h"
#include "amountcolumnadapter.h"
#include "datastore.h"
#include "formatcolumnadapter.h"
#include "formats.h"
#include "numbercolumnadapter.h"

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
        new FormatColumnAdapter<AccountSecurity, QDate>(tr("First Acquired"), &AccountSecurity::firstAcquired, dateFormat, false),
        new NumberColumnAdapter<AccountSecurity, int>(tr("Transactions"), &AccountSecurity::transactions),
        new AmountColumnAdapter<AccountSecurity, QDecNumber>(tr("Shares"), &AccountSecurity::shares, securityShares, false),
        new AmountColumnAdapter<AccountSecurity, QDecNumber>(tr("Cost Basis"), &AccountSecurity::costBasis, dollarFormat, false),
    }
{}

AccountSecurityTableModel::~AccountSecurityTableModel() {
    qDeleteAll(columns);
    for (auto i = byAccount.constBegin(); i != byAccount.constEnd(); i++) qDeleteAll(i.value());
}

void AccountSecurityTableModel::setRows(QList<const AccountSecurity*> rows) {
    beginResetModel();
    accountIds.clear();
    byAccount.clear();
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
