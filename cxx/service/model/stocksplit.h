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
    QDate date{QDate::currentDate()};
    QDecNumber sharesIn{"NaN"};
    QDecNumber sharesOut{"NaN"};

    StockSplit();
    StockSplit(const QSqlRecord &record);
    StockSplit(domain_id securityId, QDate date = QDate::currentDate(), QDecNumber sharesIn = "NaN", QDecNumber sharesOut = "NaN");

    bool deletable() const;
};

#endif // STOCKSPLIT_H
