#ifndef CATEGORY_SERVICE_H
#define CATEGORY_SERVICE_H

#include "database/connectionpool.h"
#include "database/categorydao.h"
#include "entityservice.h"

class CategoryService : public EntityService<Category, CategoryDao>
{
public:
    CategoryService(ConnectionPool *connectionPool);
};

#endif // CATEGORY_SERVICE_H
