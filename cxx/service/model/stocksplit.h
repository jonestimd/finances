#ifndef STOCKSPLIT_H
#define STOCKSPLIT_H

#include "QDecNumber.hh"
#include "basedomain.h"
#include <QDate>
#include <QSqlRecord>
#include <QVariant>

struct SplitRatio {
    QDecNumber sharesIn{1};
    QDecNumber sharesOut{1};

    SplitRatio() = default;
    SplitRatio(QDecNumber sharesIn, QDecNumber sharesOut);

    void multiply(const SplitRatio* other);
    bool isOne() const;
    QString toString() const;
};

class StockSplit : public BaseDomain, public SplitRatio {
public:
    domain_id securityId;
    QDate date{QDate::currentDate()};

    StockSplit();
    StockSplit(const QSqlRecord &record);
    StockSplit(domain_id securityId, QDate date = QDate::currentDate(), QDecNumber sharesIn = "NaN", QDecNumber sharesOut = "NaN");

    bool deletable() const;
};

#endif // STOCKSPLIT_H
