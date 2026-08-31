#ifndef SECURITY_TABLE_MODEL_H
#define SECURITY_TABLE_MODEL_H

#include <QAbstractItemModel>
#include "adapteritemmodel.h"
#include "service/model/securitylot.h"
#include "service/model/transactiondetail.h"
#include "ui/model/columnadapter.h"

class DataStore;

class SecurityLotTableModel : public ChangeTrackingItemModel {
    Q_OBJECT
    const QList<ColumnAdapter<SecurityPurchase>*> columns;
    DataStore* const dataStore;
    QList<const SecurityPurchase*> purchases{};
    QHash<domain_id, const SecurityLot*> lotsByPurchaseId{};
    QHash<domain_id, QDecNumber> sharesByPurchaseId;

public:
    const TransactionDetail* const sale;

    explicit SecurityLotTableModel(DataStore* dataStore, const TransactionDetail* sale);
    ~SecurityLotTableModel();

    QVariant headerData(int section, Qt::Orientation orientation, int role) const override;
    int columnCount(const QModelIndex &parent) const override;
    int rowCount(const QModelIndex &parent) const override;
    Qt::ItemFlags flags(const QModelIndex& index) const override;
    QVariant data(const QModelIndex &index, int role) const override;
    bool setData(const QModelIndex &index, const QVariant &value, int role) override;

    void setRows(const QList<const SecurityPurchase*> rows, const QList<const SecurityLot*> lots);

    QModelIndex index(int row, int column, const QModelIndex& parent) const override;
    QModelIndex parent(const QModelIndex& child) const override;

    const SecurityPurchase* getRow(const QModelIndex& index) const;

    bool hasUnsavedChanges() const override;
    void clearChanges() override;
    bool isValid() const override;

    /** @brief Returns shares for an existing security lot. */
    QDecNumber lotShares(domain_id purchaseId) const;
    /** @brief Returns current (unsaved) available shares for the purchase. */
    QDecNumber availableShares(const SecurityPurchase* purchaseId) const;
    /** @brief Returns current (unsaved) allocation for the purchase. */
    QDecNumber allocatedShares(domain_id purchaseId) const;
    QDecNumber totalAllocatedShares() const;

public Q_SLOTS:
    void allocateFirstIn();
    void allocateLastIn();
    void allocateLowestPrice();
    void allocateHighestPrice();
    void discardLots();

private:
    void allocateShares(std::function<bool(const SecurityPurchase*, const SecurityPurchase*)> less);
    void setAllocation(domain_id purchaseId, QDecNumber shares);
    void reset();
};

#endif // SECURITY_TABLE_MODEL_H