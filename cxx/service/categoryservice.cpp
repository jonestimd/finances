#include "categoryservice.h"
#include "service/database/transactiondetaildao.h"

CategoryService::CategoryService(ConnectionPool *connectionPool) : EntityService(connectionPool, categoryDao) {}

QList<const Category *> CategoryService::update(BulkUpdate<Category> &changes, const QString &user) {
    auto result = EntityService<Category, CategoryDao>::update(changes, user);
    if (!changes.deletes.isEmpty()) {
         // TODO add tests for updated parent returned with updated children
        QVariantList updatedIds;
        for (auto category : changes.updates) updatedIds += category->id;
        QVariantList parentIds;
        for (auto category : changes.deletes) {
            auto parentId = category->parentId;
            if (!parentId.isNull() && !updatedIds.contains(parentId)) parentIds += parentId;
        }
        if (!parentIds.isEmpty()) {
            auto conn = Connection(connectionPool);
            result += dao.get(conn.db, parentIds);
        }
    }
    return result;
}

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
