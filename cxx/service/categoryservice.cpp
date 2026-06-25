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
        QList<qlonglong> updatedIds;
        for (auto category : changes.updates) updatedIds += category->id.value();
        QList<qlonglong> parentIds;
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

QHash<qlonglong, const Category*> CategoryService::setParent(const Category *category, const std::optional<qlonglong>& parentId, const QString &user) {
    return doInTransaction<QHash<qlonglong, const Category*>>([=, this](QSqlDatabase &db) {
        return dao.setParent(db, category, parentId, user);
    });
}

QHash<qlonglong, const Category*> CategoryService::merge(const Category *category, const qlonglong destinationId, const QString &user) {
    return doInTransaction<QHash<qlonglong, const Category*>>([=, this](QSqlDatabase &db) {
        detailDao.replaceCategory(db, category, destinationId, user);
        dao.moveChildren(db, category, destinationId, user);
        dao.remove(db, QList{category});
        QList<qlonglong> ids{destinationId};
        ids.append(category->childIds);
        if (category->parentId.has_value()) ids.append(category->parentId.value());
        return dao.get(db, ids);
    });
}
