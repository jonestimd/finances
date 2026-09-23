#ifndef SECURITYLOTDAO_H
#define SECURITYLOTDAO_H

#include "entitydao.h"
#include "service/model/securitylot.h"

class TransactionDetail;

class SecurityLotDao : public EntityDao<SecurityLot> {
public:
    SecurityLotDao(const QString &dbType);

    QList<const SecurityLot*> findBySale(QSqlDatabase& db, const TransactionDetail* sale);

protected:
    virtual void bindInsertValues(QSqlQuery &query, SecurityLot *lot) override;
    virtual void bindUpdateValues(QSqlQuery &query, SecurityLot *lot) override;
};

#endif //  SECURITYLOTDAO_H