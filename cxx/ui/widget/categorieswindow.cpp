#include "categorieswindow.h"
#include "dialog.h"
// #include "settings.h"
#include <QtSql>
#include <QtWidgets>
#include <QtConcurrent>

#define LOADING_CATEGORIES "Loading categories..."
#define SAVING_CATEGORIES "Saving categories..."
#define CATEGORY_SETTINGS "categories"

CategoriesWindow::CategoriesWindow(DataStore *dataStore)
    : QMainWindow()
    , dataStore{dataStore}
    , model{dataStore, this}
    , tableSort{this, &model, tr("Categories"), tr("Name"), SLOT(saveCategories()), SLOT(loadCategories())}
{
    setCentralWidget(&tableSort.table);
    setStatusBar(&tableSort.statusBar);
    setWindowTitle(tr("Finances - Categories[*]"));

    addToolBar(&tableSort.toolbar);

    connect(dataStore, SIGNAL(categoriesLoaded(QHash<qlonglong,const Category*>)), this, SLOT(setCategories(QHash<qlonglong,const Category*>)));

    if (dataStore->loadCategories(this)) model.setRows(dataStore->categories());
    else tableSort.statusBar.addMessage(tr(LOADING_CATEGORIES));

    tableSort.setColumnResize({0});
    // tableSort.table.setRootIsDecorated(true);
    // tableSort.table.setRootIndex(QModelIndex{});
    // tableSort.table.setItemsExpandable(true);
    // tableSort.table.setTreePosition(-1);

    // settings::restoreWindowState(CATEGORY_SETTINGS, this, QSize{600, 500}, &tableSort);
}

void CategoriesWindow::loadCategories() {
    tableSort.loadData(tr(LOADING_CATEGORIES), [this]() { dataStore->loadCategories(this, true); });
}

void CategoriesWindow::saveCategories() {
    // tableSort.saveData(tr(SAVING_CATEGORIES), [this]() {
    //     dataStore->updateCategories(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
    // });
}

void CategoriesWindow::setCategories(const QHash<qlonglong, const Category*> categories) {
    model.setRows(categories);
    tableSort.statusBar.removeMessage(tr(LOADING_CATEGORIES));
    tableSort.statusBar.removeMessage(tr(SAVING_CATEGORIES));
    tableSort.table.setEnabled(true);
}

void CategoriesWindow::closeEvent(QCloseEvent *event) {
    if (!dialog::confirmDiscardChanges(this, &model)) event->ignore();
    // else settings::saveWindowState(CATEGORY_SETTINGS, this, &tableSort);
}

void CategoriesWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QMainWindow::keyPressEvent(event);
}
