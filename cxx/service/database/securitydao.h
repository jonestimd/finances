#ifndef SECURITYDAO_H
#define SECURITYDAO_H

#include "entitydao.h"
#include "service/model/security.h"

class SecurityDao : public EntityDao<Security> {
public:
    SecurityDao();

    virtual QList<const Security*> add(QSqlDatabase &db, QList<Security*> securities, const QString &user) override;
    virtual void remove(QSqlDatabase &db, QList<const Security*> securities) override;

protected:
    virtual void bindUpdateValues(QSqlQuery &query, Security *security) override;
    virtual void bindInsertValues(QSqlQuery &query, Security *security) override;
};

static SecurityDao securityDao;

#endif // SECURITYDAO_H
