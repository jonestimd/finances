#include "entityview.h"
#include "dialog.h"
#include "tableitemdelegate.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QTableWidget>
#include <QTimer>

EntityView::EntityView(QWidget *window, AdapterItemModel *model, QAbstractItemView *itemView, const QString entityName, const QString defaultSort,
                       const char *saveSlot, const char *loadSlot, QList<QAction*> actions)
    : QObject(window)
    , window{window}
    , model{model}
    , sortModel{window}
    , itemView{itemView}
    , filterInput{new FilterInput(tr("%1 filter").arg(entityName), &sortModel, window)}
    , defaultSort{defaultSort}
    , toolbar{window}
    , statusBar{window}
    , itemDelegate{window, &statusBar}
    , saveAction{finances::iconAction(finances::Save, tr("Save"), QKeySequence::Save, window, saveSlot, false)}
{
    sortModel.setSourceModel(model);
    sortModel.setSortRole(finances::SortRole);
    sortModel.setFilterKeyColumn(-1);
    sortModel.setSortCaseSensitivity(Qt::CaseInsensitive);

    itemView->setModel(&sortModel);
    itemView->setItemDelegate(&itemDelegate);
    itemView->setAlternatingRowColors(true);

    toolbar.setMovable(false);
    toolbar.addAction(addAction(tr("Add %1").arg(entityName.toLower())));
    toolbar.addAction(deleteAction(tr("Delete %1").arg(entityName.toLower()), [model](const QModelIndex &index) {
        return model->enableDelete(index);
    }));
    toolbar.addAction(undoAction());
    toolbar.addAction(saveAction);
    toolbar.addAction(finances::iconAction(finances::Refresh, tr("Reload"), QKeySequence::Refresh, window, loadSlot));

    if (!actions.isEmpty()) {
        toolbar.addSeparator();
        for (auto action : actions) {
            toolbar.addAction(action);
        }
    }
    toolbar.addWidget(filterInput);

    connect(model, SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged()));
    connect(model, SIGNAL(rowsRemoved(QModelIndex,int,int)), this, SLOT(dataChanged()));
    connect(model, SIGNAL(rowsInserted(QModelIndex,int,int)), this, SLOT(dataChanged()));
    connect(model, SIGNAL(modelReset()), this, SLOT(dataChanged()));
    connect(itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)), this, SLOT(showValidation(QModelIndex)));
    connect(itemView->itemDelegate(), &TableItemDelegate::closeEditor, this,
            [this]() { showValidation(this->itemView->selectionModel()->currentIndex()); });

    itemView->selectionModel()->select(sortModel.index(0, 0), QItemSelectionModel::Select);
}

int EntityView::columnIndex(const QString name) const {
    return model->columnIndex(name);
}

QModelIndex EntityView::selectedIndex() {
    return sortModel.mapToSource(itemView->selectionModel()->selectedIndexes().first());
}

void EntityView::enableColumnResize() {
    viewHeader()->setStretchLastSection(true);
}

void EntityView::setColumnResize(const std::vector<int> stretchColumns) {
    auto header = viewHeader();
    header->setSectionResizeMode(QHeaderView::ResizeToContents);
    for (int i : stretchColumns) {
        header->setSectionResizeMode(i, QHeaderView::Stretch);
    }
}

bool EntityView::focusFilter(QKeyEvent *event) {
    if (event->matches(QKeySequence::Find) && !filterInput->hasFocus()) {
        filterInput->setFocus();
        return true;
    }
    return false;
}

void EntityView::saveSort(QSettings *settings) {
    auto header = viewHeader();
    if (header->sortIndicatorSection() >= 0) {
        settings->setValue("sort.column", model->headerData(header->sortIndicatorSection(), Qt::Horizontal));
        settings->setValue("sort.order", header->sortIndicatorOrder());
    }
}

void EntityView::saveSizes(QString group, QSettings *settings) {
    auto header = viewHeader();
    settings->beginGroup(QString(group).append(".columns"));
    for (int section = 0; section < header->count(); ++section) {
        auto name = model->headerData(section, Qt::Horizontal).toString();
        auto width = header->sectionSize(section);
        settings->setValue(name + ".width", width);
        settings->setValue(name + ".pos", header->visualIndex(section));
    }
    settings->endGroup();
}

void EntityView::restore(QString group, QSettings *settings) {
    auto model = itemView->model();
    auto header = viewHeader();
    auto sortColumn = settings->value(group + "/sort.column", defaultSort).toString();
    if (!sortColumn.isEmpty()) {
        auto sortOrder = settings->value(group + "/sort.order", 0).toInt();
        auto index = columnIndex(sortColumn);
        header->setSortIndicator(index, static_cast<Qt::SortOrder>(sortOrder));
    }
    for (int section = 0; section < header->count(); ++section) {
        bool ok;
        auto name = model->headerData(section, Qt::Horizontal, Qt::DisplayRole).toString();
        QString column = group + ".columns/" + name;
        auto width = settings->value(column + ".width").toInt(&ok);
        if (ok) header->resizeSection(section, width);
        auto pos = settings->value(column + ".pos").toInt(&ok);
        if (ok) header->moveSection(header->visualIndex(section), pos);
    }
}

bool selectEditColumn(QModelIndex &index) {
    auto columnCount = index.model()->columnCount();
    while (index.column() < columnCount) {
        if ((index.flags() & Qt::ItemIsEditable) && !index.data().isValid()) return true;
        index = index.sibling(index.row(), index.column()+1);
    }
    return false;
}

void EntityView::startEdit(int rowIndex) {
    auto index = sortModel.index(rowIndex, 0);
    if (selectEditColumn(index)) {
        itemView->selectionModel()->setCurrentIndex(index, QItemSelectionModel::ClearAndSelect);
        itemView->edit(index);
    }
}

void EntityView::loadData(QString statusMessage, std::function<void ()> doLoad) {
    if (!dialog::confirmDiscardChanges(window, model)) return;
    itemView->setEnabled(false); // TODO save/restore selection
    statusBar.addMessage(statusMessage);
    doLoad();
}

void EntityView::saveData(QString statusMessage, std::function<void ()> doSave) {
    statusBar.addMessage(statusMessage);
    itemView->setEnabled(false);
    doSave();
}

QAction *EntityView::addAction(const QString text) {
    auto addAction = finances::iconAction(finances::AddCircle, text, QKeySequence::New, this, SLOT(addRow()));
    connect(&itemDelegate, &TableItemDelegate::openEditor, addAction, [=]() { addAction->setEnabled(false); });
    connect(&itemDelegate, &TableItemDelegate::closeEditor, addAction, [=]() { addAction->setEnabled(true); });
    return addAction;
}

QAction *EntityView::deleteAction(const QString text, std::function<bool(const QModelIndex &)> enableDelete) {
    auto action = finances::iconAction(finances::Trash, text, QKeySequence::Delete, this, SLOT(queueDeletes()));
    auto setEnabled = [=, this]() {
        auto indexes = sortModel.mapSelectionToSource(itemView->selectionModel()->selection()).indexes();
        bool enabled = !indexes.empty();
        for (auto i = indexes.cbegin(); enabled && i != indexes.cend(); ++i) enabled &= enableDelete(*i);
        action->setEnabled(enabled);
    };
    setEnabled();
    connect(itemView->selectionModel(), &QItemSelectionModel::selectionChanged, this, setEnabled);
    return action;
}

QAction *EntityView::undoAction() {
    auto undoAction = finances::iconAction(finances::Undo, tr("Undo"), QKeySequence::Undo, this, SLOT(undoChanges()));
    return undoAction;
}

void EntityView::dataChanged() {
    saveAction->setEnabled(model->hasUnsavedChanges() && model->isValid());
    window->setWindowModified(model->hasUnsavedChanges());
}

void EntityView::addRow() {
    int rowIndex = sortModel.mapFromSource(model->index(model->queueAdd(), 0)).row();
    startEdit(rowIndex);
}

void EntityView::queueDeletes() {
    auto selection = sortModel.mapSelectionToSource(itemView->selectionModel()->selection());
    for (auto i : selection.indexes()) model->queueDelete(i);
}

void EntityView::undoChanges() {
    auto selection = sortModel.mapSelectionToSource(itemView->selectionModel()->selection());
    for (auto i : selection.indexes()) model->undoChange(i);
}

void EntityView::showValidation(const QModelIndex &index) {
    // make sure index is in selection
    if (!itemView->selectionModel()->hasSelection()) itemView->selectionModel()->select(index, QItemSelectionModel::Select);
    auto message = index.data(finances::ValidationMessageRole);
    if (!message.isNull()) statusBar.showMessage(message.toString());
    else statusBar.clearMessage();
}

EntityTable::EntityTable(QWidget *window, AdapterItemModel *model, const QString filterLabel, const QString defaultSort,
                         const char *saveSlot, const char *loadSlot, QList<QAction *> actions)
    : EntityView(window, model, new QTableView(), filterLabel, defaultSort, saveSlot, loadSlot, actions)
{
    auto view = tableView();
    view->resizeColumnsToContents();
    view->setSortingEnabled(true);
    // view->verticalHeader()->setDefaultSectionSize(5); // minimize row height

    auto header = view->horizontalHeader();
    header->setSectionsMovable(true);
    header->setSortIndicatorShown(true);
    header->setSortIndicator(0, Qt::SortOrder::AscendingOrder);
}

EntityTable::~EntityTable() {
    delete itemView;
}

QTableView *EntityTable::tableView() const {
    return static_cast<QTableView*>(itemView);
}

QHeaderView *EntityTable::viewHeader() const {
    return tableView()->horizontalHeader();
}

EntityTree::EntityTree(QWidget *window, AdapterItemModel *model, const QString filterLabel, const QString defaultSort,
                         const char *saveSlot, const char *loadSlot, QList<QAction *> actions)
    : EntityView(window, model, new QTreeView(), filterLabel, defaultSort, saveSlot, loadSlot, actions)
{
    using enum QAbstractItemView::EditTrigger;
    auto view = treeView();
    view->setSortingEnabled(true);
    view->setSelectionBehavior(QAbstractItemView::SelectItems);
    view->setEditTriggers(AllEditTriggers ^ CurrentChanged);

    auto header = view->header();
    header->setSectionsMovable(true);
    header->setSortIndicatorShown(true);
    header->setSortIndicator(0, Qt::SortOrder::AscendingOrder);
}

EntityTree::~EntityTree() {
    delete itemView;
}

QTreeView *EntityTree::treeView() const {
    return static_cast<QTreeView*>(itemView);
}

QHeaderView *EntityTree::viewHeader() const {
    return treeView()->header();
}
