#include <QMessageBox>
#include <QSqlError>
#include <iostream>

#include "dbcontext.h"
#define DB_NAME "finances_test"

DbContext::DbContext(const QString &dbType, const QString &host, const QString &schema, const QString &user, const QString &password) {
    name = QString("%1:%2:%3").arg(dbType, host, schema);
    db = QSqlDatabase::addDatabase(dbType, name);
    db.setHostName(host);
    db.setDatabaseName(schema);
    db.setNumericalPrecisionPolicy(QSql::HighPrecision);
    if (!db.open(user, password)) {
        QMessageBox::critical(nullptr, QObject::tr("Cannot open database"),
                              QObject::tr("Unable to establish a database connection."),
                              QMessageBox::Cancel);
        std::cerr << db.lastError().text().data() << std::endl;
        throw "failed to connect to db";
    }

    companyDao = new CompanyDao(db);
    accountDao = new AccountDao(db);
};

DbContext::~DbContext() {
    db.close();
    QSqlDatabase::removeDatabase(name);
    delete companyDao;
    delete accountDao;
}
