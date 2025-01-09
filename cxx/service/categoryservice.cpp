#include "categoryservice.h"
#include "service/database/transactiondetaildao.h"

CategoryService::CategoryService(ConnectionPool *connectionPool) : EntityService(connectionPool, categoryDao) {}

QList<const Category*> CategoryService::setParent(const Category *category, const QVariant parentId, const QString &user) {
    return doInTransaction([=, this](QSqlDatabase &db) {
        return dao.setParent(db, category, parentId, user);
    });
}

QList<const Category *> CategoryService::merge(const Category *category, const QVariant destinationId, const QString &user) {
    return doInTransaction([=, this](QSqlDatabase &db) {
        transactionDetailDao.replaceCategory(db, category, destinationId, user);
        auto updatedCategories = dao.merge(db, category, destinationId, user);
        if (!category->parentId.isNull()) {
            updatedCategories += categoryDao.get(db, QVariantList{category->parentId});
        }
        return updatedCategories;
    });
}
