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
    QDecNumber clearedBalance_{0};

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
    void setRows(const QList<qlonglong> transactionIds);

    QVariant balance(const QVariant &transactionId) const;
    QDecNumber clearedBalance() const;

    QModelIndex index(int row, int column, const QModelIndex &parent) const override;
    QModelIndex parent(const QModelIndex &child) const override;
    int rowCount(const QModelIndex &parent) const override;

    const Transaction *getRow(const QModelIndex &index) const override;

    QVariant data(const QModelIndex &index, int role) const override;
    bool setData(const QModelIndex &index, const QVariant &value, int role) override;

    QVariant headerData(int section, Qt::Orientation orientation, int role) const override;

    Q_SIGNAL void clearedBalanceChanged(const QDecNumber &clearedBalance);

private:
    Q_SLOT void accountLoaded(qlonglong accountId);
    bool isBoldColumn(int column) const;
    static QFont boldFont();
};

#endif // TRANSACTIONTABLEMODEL_H
