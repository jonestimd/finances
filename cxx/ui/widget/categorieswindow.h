#ifndef CATEGORIES_WINDOW_H
#define CATEGORIES_WINDOW_H

#include "../model/datastore.h"
#include "../model/categorytablemodel.h"
#include "entityview.h"
#include <QMainWindow>
#include <QTableView>

class CategoriesWindow : public QMainWindow {
    Q_OBJECT
    DataStore *dataStore;
    CategoryTableModel model;
    EntityTree tableSort;
    QAction *moveAction;

public:
    CategoriesWindow(DataStore *dataStore);

public Q_SLOTS:
    void loadCategories();
    void saveCategories();
    void setCategories(const QList<qlonglong> categoryIds);
    void reparent();
    void selectionChanged(const QModelIndex &current, const QModelIndex &previous);

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);
};
#endif // CATEGORIES_WINDOW_H
