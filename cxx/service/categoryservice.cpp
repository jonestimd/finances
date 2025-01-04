#include "categoryservice.h"

CategoryService::CategoryService(ConnectionPool *connectionPool) : EntityService(connectionPool, categoryDao) {}

QList<const Category*> CategoryService::setParent(const Category *category, const QVariant parentId, const QString user) {
    auto conn = Connection(connectionPool);
    try {
        return dao->setParent(conn.db, category, parentId, user);
    } catch(...) {
        conn.db.rollback();
        throw;
    }
}
