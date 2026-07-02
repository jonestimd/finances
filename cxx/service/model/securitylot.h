#ifndef SECURITYLOT_H
#define SECURITYLOT_H

#include "QDecNumber.hh"
#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class SecurityLot : public BaseDomain {
public:
    QDecNumber purchaseShares;
    QDecNumber adjustedShares;
    domain_id purchaseDetailId;
    domain_id relatedDetailId;

    SecurityLot();
    SecurityLot(const QSqlRecord &record);
};

#endif // SECURITYLOT_H
