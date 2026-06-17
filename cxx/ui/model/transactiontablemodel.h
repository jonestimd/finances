#ifndef TRANSACTIONTABLEMODEL_H
#define TRANSACTIONTABLEMODEL_H

#include "detailcolumnadapter.h"
#include "poditemmodel.h"
#include "service/model/transaction.h"
#include "ui/model/datastore.h"

class TransactionTableModel : public PodItemModel<Transaction, PendingTransaction> {
    Q_OBJECT
    TransactionTypeColumnAdapter *const transactionTypeAdapter;
    const TransactionStore *const store;

    QHash<int, QList<TransactionDetail*>> newDetails{};

    QHash<qlonglong, QVariant> balances{};
    QDecNumber clearedBalance_{0};

public:
    const int dateColumn;
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
    void setDetailValue(TransactionDetail* detail, int columnIndex, const QVariant &value);

    int childCount(const QModelIndex &index) const override;

    PendingTransaction *pendingTransaction(const QModelIndex &index) const;
    const QList<TransactionDetail*> pendingDetails(const QModelIndex &parent) const;

    void updateBalances(int fromRow, const QDecNumber &delta);
    Q_SLOT void updateBalances();
    void updateClearedBalance(const QDecNumber &delta);

    virtual PendingTransaction *newRow() override;

public:
    void setRows(const QList<qlonglong> transactionIds);

    QVariant balance(const QVariant &transactionId) const;
    QDecNumber clearedBalance() const;

    QModelIndex index(int row, int column, const QModelIndex &parent = QModelIndex()) const override;
    QModelIndex parent(const QModelIndex &child) const override;
    int rowCount(const QModelIndex &parent = QModelIndex()) const override;

    virtual int rowType(const QModelIndex &index) const override;

    const Transaction *getRow(const QModelIndex &index) const override;
    const TransactionDetail *getDetail(const QModelIndex &index) const;

    AbstractColumnAdapter *adapter(const QModelIndex &index) const override;

    bool isPendingAdd(const QModelIndex &index) const override;
    bool isPendingDelete(const QModelIndex &index) const override;

    QVariant data(const QModelIndex &index, int role = Qt::DisplayRole) const override;
    bool setData(const QModelIndex &index, const QVariant &value, int role) override;
    Qt::ItemFlags flags(const QModelIndex &index) const override;

    QVariant headerData(int section, Qt::Orientation orientation, int role) const override;

    bool hasUnsavedChanges() const override;
    void clearChanges() override;
    bool enableDelete(const QModelIndex &index) const override;

    /**
     *  @brief transactionHasChanges Checks if the transaction associated with `index` has unsaved changes.
     *  @param index Model index of the transaction or one of its details.
     */
    bool transactionHasChanges(const QModelIndex &index) const;
    /**
     *  @brief transactionIsValid Checks if the transaction associated with `index` is valid.
     *  @param index Model index of the transaction or one of its details.
     */
    bool transactionIsValid(const QModelIndex &index) const;

    QList<const TransactionDetail*> unsavedDetailAdds(int txRow = -1) const;
    QList<TransactionDetail*> unsavedDetailChanges(int txRow = -1);
    QList<const TransactionDetail*> unsavedDetailDeletes(int txRow = -1) const;

private slots:
    void accountLoaded(qlonglong accountId);
    void accountUpdated(qlonglong accountId);
    void payeesUpdated();
    void transactionsSaved(const QList<const PendingTransaction*>& transactions);
    void transactionAdded(qlonglong accountId, int index);
    void transactionRemoved(qlonglong accountId, int index);
    void transactionUpdated(qlonglong accountId, int index, int oldDetailCount);

private:
    void queueAddTransaction();

    int pendingDeleteCount(const QModelIndex &parent) const;

    bool isBoldColumn(int column) const;
    static QFont boldFont();

signals:
    void dataLoaded();
    void clearedBalanceChanged(const QDecNumber &clearedBalance);

public slots:
    /** @brief queueAdd Called by toolbar action to add a detail row. */
    QModelIndex queueAdd(const QModelIndex &parent) override;
    void queueDelete(const QModelIndex &index) override;
    void undoChange(const QModelIndex &index) override;
};

#endif // TRANSACTIONTABLEMODEL_H
