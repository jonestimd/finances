#ifndef ACCOUNT_SECURITY_MODEL_H
#define ACCOUNT_SECURITY_MODEL_H

#include <QAbstractItemModel>
#include "datastore.h"

class AccountSecurityTableModel : public QAbstractItemModel {
    Q_OBJECT
    const QList<ColumnAdapter<AccountSecurity>*> columns;
    QHash<domain_id, QList<const AccountSecurity*>> byAccount{};
    QList<domain_id> accountIds{};
    AccountStore* accountStore;
    SecurityStore* securityStore;

public:
    explicit AccountSecurityTableModel(DataStore* dataStore);
    ~AccountSecurityTableModel();

private:
    void reset();

public:
    virtual QVariant headerData(int section, Qt::Orientation orientation, int role) const override;
    virtual QModelIndex index(int row, int column, const QModelIndex &parent = {}) const override;
    virtual QModelIndex parent(const QModelIndex &child) const override;
    virtual int rowCount(const QModelIndex &parent) const override;
    virtual int columnCount(const QModelIndex &parent) const override;
    virtual QVariant data(const QModelIndex &index, int role) const override;

public slots:
    void setRows(QList<const AccountSecurity*> rows); // clazy:exclude=fully-qualified-moc-types
    void updateRows(QList<const AccountSecurity*> rows); // clazy:exclude=fully-qualified-moc-types
    void removeRows(const QList<AccountSecurityId> ids);

private:
    /** @return index of account security summary or index of parent account if there is no security summary. */
    QModelIndex indexOf(AccountSecurityId id);
};

#endif // ACCOUNT_SECURITY_MODEL_H
