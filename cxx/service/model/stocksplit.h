#ifndef STOCKSPLIT_H
#define STOCKSPLIT_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class StockSplit : public BaseDomain {
public:
    QVariant securityId;
    QVariant date;
    QVariant sharesIn;
    QVariant sharesOut;

    StockSplit();
    StockSplit(const QSqlRecord &record);
};

#endif // STOCKSPLIT_H
