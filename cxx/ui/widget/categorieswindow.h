#ifndef CATEGORIES_WINDOW_H
#define CATEGORIES_WINDOW_H

#include "../model/datastore.h"
#include "../model/categorytablemodel.h"
#include "appwindow.h"
#include <QMainWindow>
#include <QTableView>
#include <ui/model/comboboxmodel.h>

class CategoriesWindow : public AppWindow {
    Q_OBJECT
    CategoryStore *store;
    QAction *moveAction;
    QAction *mergeAction;
    ComboBoxModel::GetName getName;

public:
    CategoriesWindow(DataStore *dataStore);

    CategoryTableModel *model();

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void setCategories(const QList<qlonglong> categoryIds);
    void reparent();
    void merge();
    void selectionChanged();
};
#endif // CATEGORIES_WINDOW_H
