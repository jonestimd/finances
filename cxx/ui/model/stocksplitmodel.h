#ifndef STOCK_SPLIT_MODEL_H
#define STOCK_SPLIT_MODEL_H

#include "service/model/stocksplit.h"
#include "podtablemodel.h"
#include "ui/store/stocksplitstore.h"

class StockSplitTableModel : public PodTableModel<StockSplit, StockSplitStore> {
    Q_OBJECT
    domain_id securityId;
public:
    explicit StockSplitTableModel(const StockSplitStore *store, domain_id securityId, QObject *parent = nullptr);

    void setRows();

protected:
    StockSplit* newRow() override;
};

#endif // STOCK_SPLIT_MODEL_H
