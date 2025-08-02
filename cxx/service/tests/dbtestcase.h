#ifndef DB_TEST_CASE_H
#define DB_TEST_CASE_H

#include "../database/connectionpool.h"
#include <qvariant.h>

#define TEST_USER "test"

class DbTestCase {
    QHash<QString, ConnectionPool*> connectionPools{};

public:
    DbTestCase();

    QList<QString> connectionPoolNames();

    ConnectionPool *connectionPool(QString name);

    void createDatabases();

    QVariant addCompany(QString driver, const QString &name);
    QVariant addAccount(QString driver, const QString &name, const QString &type, const QVariant companyId = QVariant{});
    QVariant addPayee(QString driver, const QString &name);

private:
    void addConnection(QString name, const ConnectionSettings &settings);
};

#endif // DB_TEST_CASE_H
