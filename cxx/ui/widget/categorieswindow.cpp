#include "categorieswindow.h"
#include "entityselectiondialog.h"
#include "settings.h"
#include "treeview.h"

#define LOADING_CATEGORIES "Loading categories..."
#define SAVING_CATEGORIES "Saving categories..."
#define CATEGORY_SETTINGS "categories"

CategoriesWindow::CategoriesWindow(DataStore *dataStore)
    : AppWindow{tr("Category"), new CategoryTableModel(dataStore), new TreeView(), CATEGORY_SETTINGS}
    , store{dataStore->categoryStore}
    , moveAction{finances::iconAction(finances::MoveUp, tr("Change parent"), tr("ctrl+m", "reparent category"), this, SLOT(reparent()), false)}
    , mergeAction{finances::iconAction(finances::MergeType, tr("Merge Categories"), tr("ctrl+y", "merge category"), this, SLOT(merge()), false)}
    , getName{[this](const NamedEntity* entity) {
        return store->displayName(entity->id.toLongLong());
    }}
{
    setWindowTitle(tr("%1 - Categories[*]").arg(dataStore->connectionName()));

    // TODO disable with pending changes
    entityView.insertAction(2, moveAction);
    entityView.insertAction(3, mergeAction);

    connect(entityView.itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)),
            this, SLOT(selectionChanged(QModelIndex,QModelIndex)));

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setCategories(QList<qlonglong>)));

    if (store->load(this)) model()->setRows(store->ids());
    else disableUi(tr(LOADING_CATEGORIES));

    settings::restoreWindowState(CATEGORY_SETTINGS, this, QSize{600, 500}, &entityView);
}

CategoryTableModel *CategoriesWindow::model() {
    return static_cast<CategoryTableModel*>(entityView.model);
}

void CategoriesWindow::loadData() {
    if (entityView.confirmLoadData(tr(LOADING_CATEGORIES))) store->load(this, true);
}

void CategoriesWindow::saveData() {
    disableUi(tr(SAVING_CATEGORIES));
    store->update(this, model());
}

void CategoriesWindow::setCategories(const QList<qlonglong> categoryIds) {
    model()->setRows(categoryIds);
    entityView.enableUi();
}

void CategoriesWindow::reparent() {
    auto category = model()->getRow(entityView.selectedIndex());
    auto name = category->name.toString();
    QList<const NamedEntity*> options;
    QHash<qlonglong, QString> disabledOptions;
    for (auto id : store->ids()) {
        auto option = store->value(id);
        auto message = tr("\"%1\" already has a child named \"%2\"").arg(option->name.toString(), name);
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
            disableUi(tr(SAVING_CATEGORIES));
            store->setParent(this, category, parentId);
        }
    }
}

void CategoriesWindow::merge() {
    auto category = model()->getRow(entityView.selectedIndex());
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
            disableUi(tr(SAVING_CATEGORIES));
            store->mergeCategories(this, category, selectedId);
        }
    }
}

void CategoriesWindow::selectionChanged(const QModelIndex &current, const QModelIndex &previous) {
    moveAction->setEnabled(model()->movable(entityView.selectedIndex()));
    mergeAction->setEnabled(entityView.selectedIndex().isValid());
}
