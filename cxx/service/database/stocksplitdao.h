#ifndef STOCKSPLITDAO_H
#define STOCKSPLITDAO_H

#include "entitydao.h"
#include "service/model/stocksplit.h"

class StockSplitDao : public EntityDao<StockSplit> {
public:
    StockSplitDao(const QString &dbType);

protected:
    virtual void bindInsertValues(QSqlQuery &query, StockSplit *split) override;
    virtual void bindUpdateValues(QSqlQuery &query, StockSplit *split) override;
};

#endif //  STOCKSPLITDAO_H