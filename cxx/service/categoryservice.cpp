#include "categoryservice.h"
#include "service/database/transactiondetaildao.h"

CategoryService::CategoryService(ConnectionPool *connectionPool, CategoryDao &dao, TransactionDetailDao &detailDao)
    : EntityService(connectionPool, dao)
    , detailDao{detailDao}
{}

QList<const Category *> CategoryService::update(BulkUpdate<Category> &changes, const QString &user) {
    auto result = EntityService<Category, CategoryDao>::update(changes, user);
    if (!changes.deletes.isEmpty()) {
         // TODO add tests for updated parent returned with updated children
        QList<domain_id> updatedIds;
        for (auto category : changes.updates) updatedIds += category->id.value();
        QList<domain_id> parentIds;
        for (auto category : changes.deletes) {
            auto parentId = category->parentId;
            if (parentId.has_value() && !updatedIds.contains(parentId.value())) parentIds += parentId.value();
        }
        if (!parentIds.isEmpty()) {
            auto conn = Connection(connectionPool);
            result += dao.get(conn.db, parentIds).values();
        }
    }
    return result;
}

QHash<domain_id, const Category*> CategoryService::setParent(const Category *category, const optional_id& parentId, const QString &user) {
    return doInTransaction<QHash<domain_id, const Category*>>([=, this](QSqlDatabase &db) {
        return dao.setParent(db, category, parentId, user);
    });
}

QHash<domain_id, const Category*> CategoryService::merge(const Category *category, const domain_id destinationId, const QString &user) {
    return doInTransaction<QHash<domain_id, const Category*>>([=, this](QSqlDatabase &db) {
        detailDao.replaceCategory(db, category, destinationId, user);
        dao.moveChildren(db, category, destinationId, user);
        dao.remove(db, QList{category});
        QList<domain_id> ids{destinationId};
        ids.append(category->childIds);
        if (category->parentId.has_value()) ids.append(category->parentId.value());
        return dao.get(db, ids);
    });
}
