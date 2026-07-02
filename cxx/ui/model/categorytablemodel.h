#ifndef CATEGORY_TABLE_MODEL_H
#define CATEGORY_TABLE_MODEL_H

#include "datastore.h"
#include "poditemmodel.h"
#include "service/model/category.h"
#include <QAbstractTableModel>

class CategoryTableModel : public PodItemModel<Category, CategoryStore> {
    Q_OBJECT
protected:
    int childCount(const QModelIndex &parent) const override;

public:
    explicit CategoryTableModel(DataStore *datastore);

    void setRows(QList<domain_id> categoryIds) override;
    const Category* getRow(const QModelIndex &index) const override;
    int rowCount(const QModelIndex &parent = QModelIndex()) const override;
    bool movable(const QModelIndex &index);

    // QAbstractItemModel interface
    virtual QModelIndex index(int row, int column, const QModelIndex &parent) const override;
    virtual QModelIndex parent(const QModelIndex &child) const override;
};

#endif // CATEGORY_TABLE_MODEL_H
