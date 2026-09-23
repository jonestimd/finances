#include "stocksplit.h"
#include "service/database/sql.h"

SplitRatio::SplitRatio(QDecNumber sharesIn, QDecNumber sharesOut)
    : sharesIn{sharesIn}, sharesOut{sharesOut} {}

void SplitRatio::multiply(const SplitRatio* other) {
    sharesIn = sharesIn * other->sharesIn;
    sharesOut = sharesOut * other->sharesOut;
}

bool SplitRatio::isOne() const {
    return sharesIn == sharesOut;
}

QString SplitRatio::toString() const {
    auto display = QString("×%1").arg(QDecNumber{sharesOut}.trim().toString());
    if (sharesIn != QDecNumber{1}) display += "/" + QDecNumber{sharesIn}.trim().toString();
    return display;
}

StockSplit::StockSplit() {}

StockSplit::StockSplit(const QSqlRecord &record)
    : BaseDomain{record}
    , SplitRatio{sql::decimalValue(record, "shares_in").value(), sql::decimalValue(record, "shares_out").value()}
    , securityId{sql::getValue(record, "security_id").toLongLong()}
    , date{sql::getDate(record, "date").value()}
{}

StockSplit::StockSplit(domain_id securityId, QDate date, QDecNumber sharesIn, QDecNumber sharesOut)
    : SplitRatio{sharesIn, sharesOut}
    , securityId{securityId}
    , date{date}
{}

bool StockSplit::deletable() const {
    return true;
}
