#ifndef TRANSACTIONTABLEMODEL_H
#define TRANSACTIONTABLEMODEL_H

#include "poditemmodel.h"
#include "service/model/transaction.h"
#include "ui/model/datastore.h"

class TransactionTableModel : public PodItemModel<Transaction> {
    Q_OBJECT
    const QList<ColumnAdapter<TransactionDetail>*> detailColumns;
    const TransactionStore *const store;
    QHash<qlonglong, QVariant> balances{};

public:
    const int payeeColumn;
    const int securityColumn;
    const int clearedColumn;
    const int subtotalColumn;
    const qlonglong accountId;

    explicit TransactionTableModel(DataStore *dataStore, qlonglong accountId);
    ~TransactionTableModel();

protected:
    QList<qlonglong> transactionIds() const;
    int childCount(const QModelIndex &index) const override;

public:
    void setRows(QList<qlonglong> transactionIds);

    QVariant balance(const QVariant &transactionId) const;

    QModelIndex index(int row, int column, const QModelIndex &parent) const override;
    QModelIndex parent(const QModelIndex &child) const override;
    int rowCount(const QModelIndex &parent) const override;

    const Transaction *getRow(const QModelIndex &index) const override;

    virtual QVariant data(const QModelIndex &index, int role) const override;
    QVariant headerData(int section, Qt::Orientation orientation, int role) const override;

private:
    Q_SLOT void accountLoaded(qlonglong accountId);
    bool isBoldColumn(int column) const;
    static QFont boldFont();
};

#endif // TRANSACTIONTABLEMODEL_H
