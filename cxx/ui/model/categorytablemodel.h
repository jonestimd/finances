#ifndef CATEGORY_TABLE_MODEL_H
#define CATEGORY_TABLE_MODEL_H

#include "datastore.h"
#include "poditemmodel.h"
#include "service/model/category.h"
#include <QAbstractTableModel>

class CategoryTableModel : public PodItemModel<Category> {
    Q_OBJECT
    DataStore* dataStore;
    QHash<qlonglong, const Category*> categories;
    QList<QVariant> rootIds;

    const Category* row(const QModelIndex &index) const;

protected:
    int childCount(const QModelIndex &parent) const override;

public:
    explicit CategoryTableModel(DataStore *datastore, QObject *parent);

    void setRows(QHash<qlonglong, const Category*> categories);
    const Category* getRow(const QModelIndex &index) const override;
    int rowCount(const QModelIndex &parent = QModelIndex()) const override;

    const QList<Category*> unsavedAdds() const;

    // QAbstractItemModel interface
    virtual QModelIndex index(int row, int column, const QModelIndex &parent) const override;
    virtual QModelIndex parent(const QModelIndex &child) const override;
};

#endif // CATEGORY_TABLE_MODEL_H
