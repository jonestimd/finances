#ifndef CATEGORY_DAO_H
#define CATEGORY_DAO_H

#include "entitydao.h"
#include "../model/category.h"
#include <QtSql/QSqlDatabase>

class CategoryDao : public EntityDao<Category> {

public:
    CategoryDao();

    QList<const Category*> setParent(QSqlDatabase &db, const Category* category, const QVariant parentId, const QString user);
    void moveChildren(QSqlDatabase &db, const Category* category, const QVariant destinationId, const QString user);

protected:
    virtual void bindUpdateValues(QSqlQuery &query, Category *entity) override;
    virtual void bindInsertValues(QSqlQuery &query, Category *entity) override;
};

static CategoryDao categoryDao;

#endif // CATEGORY_DAO_H
