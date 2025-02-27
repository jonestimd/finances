#ifndef TRANSACTIONTABLEMODEL_H
#define TRANSACTIONTABLEMODEL_H

#include "detailcolumnadapter.h"
#include "poditemmodel.h"
#include "service/model/transaction.h"
#include "ui/model/datastore.h"

class TransactionTableModel : public PodItemModel<Transaction> {
    Q_OBJECT
    TransactionTypeColumnAdapter *const transactionTypeAdapter;
    const TransactionStore *const store;
    QMultiHash<int, TransactionDetailUpdate*> newDetails{};
    QHash<qlonglong, QVariant> balances{};
    QDecNumber clearedBalance_{0};

public:
    const int payeeColumn;
    const int securityColumn;
    const int clearedColumn;
    const int subtotalColumn;
    const int balanceColumn;
    const qlonglong accountId;

private:
    const QList<ColumnAdapter<TransactionDetail>*> detailColumns;

public:
    explicit TransactionTableModel(DataStore *dataStore, qlonglong accountId);
    ~TransactionTableModel();

protected:
    const QList<qlonglong> transactionIds() const;

    QVariant value(const QModelIndex &index, int role, QVariant current) const override;
    void setValue(const QModelIndex &index, const QVariant &value) override;

    int childCount(const QModelIndex &index) const override;

    QList<TransactionDetailUpdate*> pendingDetails(const QModelIndex &parent) const;

    void updateBalances(int fromRow, const QDecNumber &delta);
    Q_SLOT void updateBalances();
    void updateClearedBalance(const QDecNumber &delta);

public:
    void setRows(const QList<qlonglong> transactionIds);

    QVariant balance(const QVariant &transactionId) const;
    QDecNumber clearedBalance() const;

    QModelIndex index(int row, int column, const QModelIndex &parent = QModelIndex()) const override;
    QModelIndex parent(const QModelIndex &child) const override;
    int rowCount(const QModelIndex &parent = QModelIndex()) const override;

    const Transaction *getRow(const QModelIndex &index) const override;
    const TransactionDetail *getDetail(const QModelIndex &index) const;

    AbstractColumnAdapter *adapter(const QModelIndex &index) const override;

    QVariant data(const QModelIndex &index, int role = Qt::DisplayRole) const override;
    bool setData(const QModelIndex &index, const QVariant &value, int role) override;
    Qt::ItemFlags flags(const QModelIndex &index) const override;

    QVariant headerData(int section, Qt::Orientation orientation, int role) const override;

    // bool hasUnsavedChanges() const override;
    // void clearChanges() override;
    // bool enableDelete(const QModelIndex &index) const override;

    const QList<TransactionDetailUpdate*> unsavedDetailAdds() const;
    const QList<TransactionDetailUpdate*> unsavedDetailChanges() const;
    const QList<const TransactionDetail*> unsavedDetailDeletes() const;

private:
    Q_SLOT void accountLoaded(qlonglong accountId);

    bool isBoldColumn(int column) const;
    static QFont boldFont();

Q_SIGNALS:
    void clearedBalanceChanged(const QDecNumber &clearedBalance);

public Q_SLOTS:
    // int queueAdd(const QModelIndex &parent) override;
    // void queueDelete(const QModelIndex &index) override;
    void undoChange(const QModelIndex &index) override;
};

#endif // TRANSACTIONTABLEMODEL_H
