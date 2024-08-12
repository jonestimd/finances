#ifndef CATEGORY_DAO_H
#define CATEGORY_DAO_H

#include "entitydao.h"
#include "../model/category.h"
#include <QtSql/QSqlDatabase>

class CategoryDao : public QObject, public EntityDao<Category> {
    Q_OBJECT
public:
    CategoryDao();

    QList<const Category*> getAll(QSqlDatabase &db) override;

protected:
    virtual void bindUpdateValues(QSqlQuery &query, Category *entity) override;
    virtual void bindInsertValues(QSqlQuery &query, Category *entity) override;
};

Q_GLOBAL_STATIC(CategoryDao, categoryDao)

#endif // CATEGORY_DAO_H
