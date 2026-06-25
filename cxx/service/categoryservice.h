#ifndef CATEGORY_SERVICE_H
#define CATEGORY_SERVICE_H

#include "database/connectionpool.h"
#include "database/categorydao.h"
#include "database/transactiondetaildao.h"
#include "entityservice.h"

class CategoryService : public EntityService<Category, CategoryDao> {
    TransactionDetailDao &detailDao;

public:
    CategoryService(ConnectionPool *connectionPool, CategoryDao &dao, TransactionDetailDao &detailDao);

    virtual QList<const Category*> update(BulkUpdate<Category> &changes, const QString &user) override;

    QHash<qlonglong, const Category*> setParent(const Category *category, const std::optional<qlonglong>& parentId, const QString &user);
    // TODO return updated tx details?
    QHash<qlonglong, const Category *> merge(const Category *category, const qlonglong destinationId, const QString &user);
};

#endif // CATEGORY_SERVICE_H
