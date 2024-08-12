#ifndef CATEGORY_TABLE_MODEL_H
#define CATEGORY_TABLE_MODEL_H

#include "datastore.h"
#include "podtablemodel.h"
#include "service/model/category.h"
#include <QAbstractTableModel>

class CategoryTableModel : public PodTableModel<Category> {
    Q_OBJECT
    DataStore *dataStore;
public:
    explicit CategoryTableModel(DataStore *datastore, QObject *parent);
};

#endif // CATEGORY_TABLE_MODEL_H
