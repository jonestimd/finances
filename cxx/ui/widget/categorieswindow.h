#ifndef CATEGORIES_WINDOW_H
#define CATEGORIES_WINDOW_H

#include "../model/datastore.h"
#include "../model/categorytablemodel.h"
#include "entityview.h"
#include "statuswindow.h"
#include <QMainWindow>
#include <QTableView>
#include <ui/model/comboboxmodel.h>

class CategoriesWindow : public StatusWindow {
    Q_OBJECT
    CategoryStore *store;
    CategoryTableModel model;
    QTreeView *itemView{new QTreeView(this)};
    EntityView tableSort;
    QAction *moveAction;
    QAction *mergeAction;
    ComboBoxModel::GetName getName;

public:
    CategoriesWindow(DataStore *dataStore);

public Q_SLOTS:
    void loadCategories();
    void saveCategories();
    void setCategories(const QList<qlonglong> categoryIds);
    void reparent();
    void merge();
    void selectionChanged(const QModelIndex &current, const QModelIndex &previous);

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);
};
#endif // CATEGORIES_WINDOW_H
