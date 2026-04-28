#ifndef DB_TEST_CASE_H
#define DB_TEST_CASE_H

#include <QVariant>
#include "service/database/connectionpool.h"
#include "service/model/category.h"

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
    Category *addCategory(const QString &driver, const QString &name, Category *parent = nullptr);

private:
    void addConnection(QString name, const ConnectionSettings &settings);
};

#endif // DB_TEST_CASE_H
