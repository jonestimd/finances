#ifndef STOCKSPLIT_H
#define STOCKSPLIT_H

#include "QDecNumber.hh"
#include "basedomain.h"
#include <QDate>
#include <QSqlRecord>
#include <QVariant>

class StockSplit : public BaseDomain {
public:
    domain_id securityId;
    QDate date;
    QDecNumber sharesIn;
    QDecNumber sharesOut;

    StockSplit();
    StockSplit(const QSqlRecord &record);
};

#endif // STOCKSPLIT_H
