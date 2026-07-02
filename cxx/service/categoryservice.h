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

    QHash<domain_id, const Category*> setParent(const Category *category, const optional_id& parentId, const QString &user);
    // TODO return updated tx details?
    QHash<domain_id, const Category *> merge(const Category *category, const domain_id destinationId, const QString &user);
};

#endif // CATEGORY_SERVICE_H
