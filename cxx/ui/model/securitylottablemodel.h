#ifndef SECURITY_TABLE_MODEL_H
#define SECURITY_TABLE_MODEL_H

#include <QAbstractItemModel>
#include "changetrackingitemmodel.h"
#include "service/model/securitylot.h"
#include "service/model/transactiondetail.h"
#include "ui/model/columnadapter.h"

class DataStore;
class Transaction;

class SecurityLotTableModel : public ChangeTrackingItemModel {
    Q_OBJECT
    const QList<ColumnAdapter<SecurityPurchase>*> columns;
    DataStore* const dataStore;
    QList<const SecurityPurchase*> purchases{};
    QHash<domain_id, const SecurityLot*> lotsByPurchaseId{};
    QHash<domain_id, QDecNumber> sharesByPurchaseId;

public:
    const TransactionDetail* const sale;
    const Transaction* const saleTx;

    explicit SecurityLotTableModel(DataStore* dataStore, const TransactionDetail* sale);
    ~SecurityLotTableModel();

    QVariant headerData(int section, Qt::Orientation orientation, int role) const override;
    int columnCount(const QModelIndex &parent) const override;
    int rowCount(const QModelIndex &parent) const override;
    Qt::ItemFlags flags(const QModelIndex& index) const override;
    QVariant data(const QModelIndex &index, int role) const override;
    bool setData(const QModelIndex &index, const QVariant &value, int role) override;

    void setRows(const QList<const SecurityPurchase*> rows, const QList<const SecurityLot*> lots);
    void updateLots(const QList<const SecurityLot*> lots, const QList<const SecurityLot*> deletes);

    QModelIndex index(int row, int column, const QModelIndex& parent) const override;
    QModelIndex parent(const QModelIndex& child) const override;

    const SecurityPurchase* getRow(const QModelIndex& index) const;

    bool hasUnsavedChanges() const override;
    void clearChanges() override;
    bool isValid() const override;
    void undoChange(const QModelIndex& index) override;

    QList<const SecurityLot*> unsavedAdds() const;
    QList<const SecurityLot*> unsavedDeletes() const;
    QList<SecurityLot*> unsavedChanges() const;

    /** @returns Purchase shares adjusted for splits. */
    QDecNumber purchaseShares(const SecurityPurchase* purchase) const;
    /** @returns Shares for an existing security lot. */
    QDecNumber lotShares(domain_id purchaseId) const;
    /** @returns Current (unsaved) available shares for the purchase. */
    QDecNumber availableShares(const SecurityPurchase* purchaseId) const;
    /** @returns Current (unsaved) allocation for the purchase. */
    QDecNumber allocatedShares(domain_id purchaseId) const;
    QDecNumber totalAllocatedShares() const;

public Q_SLOTS:
    void allocateFirstIn();
    void allocateLastIn();
    void allocateLowestPrice();
    void allocateHighestPrice();
    void discardLots();

private:
    QDecNumber price(const SecurityPurchase* purchase) const;
    void allocateShares(std::function<bool(const SecurityPurchase*, const SecurityPurchase*)> less);
    void setAllocation(domain_id purchaseId, QDecNumber shares);
    void reset();
};

#endif // SECURITY_TABLE_MODEL_H