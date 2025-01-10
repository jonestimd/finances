#include "categorieswindow.h"
#include "entityselectiondialog.h"
#include "dialog.h"
#include "settings.h"
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
    , getName{[dataStore](const NamedEntity* entity) {
        return dataStore->categories()->displayName(entity->id.toLongLong());
    }}
{
    setCentralWidget(tableSort.itemView);
    setStatusBar(&tableSort.statusBar);
    setWindowTitle(tr("Finances - Categories[*]"));

    addToolBar(&tableSort.toolbar);
    // TODO disable with pending changes
    moveAction = finances::iconAction(finances::MoveUp, tr("Change parent"), tr("ctrl+m", "reparent category"), this, SLOT(reparent()));
    moveAction->setEnabled(false);
    tableSort.toolbar.insertAction(tableSort.toolbar.actions()[2], moveAction);

    mergeAction = finances::iconAction(finances::MergeType, tr("Merge Into"), tr("ctrl+y", "merge category"), this, SLOT(merge()));
    mergeAction->setEnabled(false);
    tableSort.toolbar.insertAction(tableSort.toolbar.actions()[3], mergeAction);

    connect(tableSort.itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)),
            this, SLOT(selectionChanged(QModelIndex,QModelIndex)));

    connect(dataStore, SIGNAL(categoriesLoaded(QList<qlonglong>)), this, SLOT(setCategories(QList<qlonglong>)));

    if (dataStore->loadCategories(this)) model.setRows(dataStore->categories()->ids());
    else tableSort.statusBar.addMessage(tr(LOADING_CATEGORIES));

    tableSort.enableColumnResize();

    settings::restoreWindowState(CATEGORY_SETTINGS, this, QSize{600, 500}, &tableSort);
}

void CategoriesWindow::enableUi() {
    tableSort.enableUi();
}

void CategoriesWindow::loadCategories() {
    tableSort.loadData(tr(LOADING_CATEGORIES), [this]() { dataStore->loadCategories(this, true); });
}

void CategoriesWindow::saveCategories() {
    tableSort.saveData(tr(SAVING_CATEGORIES), [this]() {
        dataStore->updateCategories(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
    });
}

void CategoriesWindow::setCategories(const QList<qlonglong> categoryIds) {
    model.setRows(categoryIds);
    tableSort.statusBar.removeMessage(tr(LOADING_CATEGORIES));
    tableSort.statusBar.removeMessage(tr(SAVING_CATEGORIES));
    tableSort.itemView->setEnabled(true);
}

void CategoriesWindow::reparent() {
    auto category = model.getRow(tableSort.selectedIndex());
    auto name = category->name.toString();
    auto store = dataStore->categories();
    QList<const NamedEntity*> options;
    QHash<qlonglong, QString> disabledOptions;
    for (auto id : store->ids()) {
        auto option = store->value(id);
        auto message = tr("\"%1\" already has a child named \"%2\"").arg(option->name.toString()).arg(name);
        if (option != category && !store->isAncestor(id, category->id)) {
            options.append(option);
            if (store->hasChild(id, category->name)) disabledOptions.insert(option->id.toLongLong(), message);
        }
    }
    auto model = new ComboBoxModel(options, getName);
    EntitySelectionDialog dialog(this, model, tr("Move Category"), tr("Select parent category for \"%1\":").arg(name), disabledOptions);
    if (!category->parentId.isNull()) dialog.setSelectedEntity(store->value(category->parentId.toLongLong()));
    auto result = dialog.exec();
    if (result == QDialog::Accepted) {
        auto parentId = dialog.selectedId();
        if (category->parentId != dialog.selectedId()) {
            tableSort.saveData(tr(SAVING_CATEGORIES), [this, category, parentId]() {
                dataStore->setParent(this, category, parentId);
            });
        }
    }
}

void CategoriesWindow::merge() {
    auto category = model.getRow(tableSort.selectedIndex());
    auto store = dataStore->categories();
    QList<const NamedEntity*> options;
    for (auto id : store->ids()) {
        auto option = store->value(id);
        if (option != category) options.append(option);
    }
    auto model = new ComboBoxModel(options, getName);
    EntitySelectionDialog dialog(this, model, tr("Merge Categories"), tr("Select destination category:"));
    auto result = dialog.exec();
    if (result == QDialog::Accepted) {
        auto selectedId = dialog.selectedId();
        if (!selectedId.isNull()) {
            tableSort.saveData(tr(SAVING_CATEGORIES), [this, category, selectedId]() {
                dataStore->mergeCategories(this, category, selectedId);
            });
        }
    }
}

void CategoriesWindow::selectionChanged(const QModelIndex &current, const QModelIndex &previous) {
    moveAction->setEnabled(model.movable(tableSort.selectedIndex()));
    mergeAction->setEnabled(tableSort.selectedIndex().isValid());
}

void CategoriesWindow::closeEvent(QCloseEvent *event) {
    if (!dialog::confirmDiscardChanges(this, &model)) event->ignore();
    else settings::saveWindowState(CATEGORY_SETTINGS, this, &tableSort);
}

void CategoriesWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QMainWindow::keyPressEvent(event);
}
