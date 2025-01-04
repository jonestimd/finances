#ifndef CATEGORY_SERVICE_H
#define CATEGORY_SERVICE_H

#include "database/connectionpool.h"
#include "database/categorydao.h"
#include "entityservice.h"

class CategoryService : public EntityService<Category, CategoryDao>
{
public:
    CategoryService(ConnectionPool *connectionPool);

    QList<const Category*> setParent(const Category *category, const QVariant parentId, const QString user);
};

#endif // CATEGORY_SERVICE_H
