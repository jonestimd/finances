#ifndef SECURITYDAO_H
#define SECURITYDAO_H

#include "entitydao.h"
#include "service/model/security.h"

class SecurityDao : public NamedEntityDao<Security> {
    const char *createAdjustSharesSql;
    const char *createAccountSecuritySql;

public:
    SecurityDao(const QString &dbType);

    virtual void createTable(const QSqlDatabase &db) const override;
    void createViews(const QSqlDatabase &db) const;

    virtual QList<const Security*> add(QSqlDatabase &db, QList<Security*> securities, const QString &user) override;
    virtual void remove(QSqlDatabase &db, QList<const Security*> securities) override;
    virtual QList<const Security*> update(QSqlDatabase &db, const QList<Security*> securities, const QString &user) override;

protected:
    virtual void bindUpdateValues(QSqlQuery &query, Security *security) override;
    virtual void bindInsertValues(QSqlQuery &query, Security *security) override;
};

#endif // SECURITYDAO_H
