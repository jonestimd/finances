#ifndef SECURITYLOT_H
#define SECURITYLOT_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class SecurityLot : public BaseDomain {
public:
    QVariant purchaseShares;
    QVariant adjustedShares;
    QVariant purchaseDetailId;
    QVariant relatedDetailId;

    SecurityLot();
    SecurityLot(const QSqlRecord &record);
};

#endif // SECURITYLOT_H
