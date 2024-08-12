#include "entitytree.h"
#include "dialog.h"
#include "tableitemdelegate.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QTableWidget>
#include <QTimer>

EntityTree::EntityTree(QWidget *window, AdapterItemModel *model, const QString entityName, const QString defaultSort,
                         const char *saveSlot, const char *loadSlot, QList<QAction*> actions)
    : QObject(window)
    , window{window}
    , model{model}
    , sortModel{window}
    , table{window}
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

    table.setModel(&sortModel);
    table.setItemDelegate(&itemDelegate);

    // table.resizeColumnsToContents();
    table.setAlternatingRowColors(true);
    table.setSortingEnabled(true);
    // table.verticalHeader()->setDefaultSectionSize(5); // minimize row height

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
    connect(table.selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)), this, SLOT(showValidation(QModelIndex)));
    connect(table.itemDelegate(), &TableItemDelegate::closeEditor, this,
            [this]() { showValidation(table.selectionModel()->currentIndex()); });

    auto header = table.header();
    header->setSectionsMovable(true);
    header->setSortIndicatorShown(true);
    header->setSortIndicator(0, Qt::SortOrder::AscendingOrder);

    table.selectionModel()->select(sortModel.index(0, 0), QItemSelectionModel::Select);
}

int EntityTree::columnIndex(const QString name) const {
    return model->columnIndex(name);
}

QModelIndex EntityTree::selectedIndex() {
    return sortModel.mapToSource(table.selectionModel()->selectedIndexes().first());
}

void EntityTree::enableColumnResize() {
    table.header()->setStretchLastSection(true);
}

void EntityTree::setColumnResize(const std::vector<int> stretchColumns) {
    auto header = table.header();
    header->setSectionResizeMode(QHeaderView::ResizeToContents);
    for (int i : stretchColumns) {
        header->setSectionResizeMode(i, QHeaderView::Stretch);
    }
}

bool EntityTree::focusFilter(QKeyEvent *event) {
    if (event->matches(QKeySequence::Find) && !filterInput->hasFocus()) {
        filterInput->setFocus();
        return true;
    }
    return false;
}

void EntityTree::saveSort(QSettings *settings) {
    auto header = table.header();
    if (header->sortIndicatorSection() >= 0) {
        settings->setValue("sort.column", model->headerData(header->sortIndicatorSection(), Qt::Horizontal));
        settings->setValue("sort.order", header->sortIndicatorOrder());
    }
}

void EntityTree::saveSizes(QString group, QSettings *settings) {
    auto header = table.header();
    settings->beginGroup(QString(group).append(".columns"));
    for (int section = 0; section < header->count(); ++section) {
        auto name = model->headerData(section, Qt::Horizontal).toString();
        auto width = header->sectionSize(section);
        settings->setValue(name + ".width", width);
        settings->setValue(name + ".pos", header->visualIndex(section));
    }
    settings->endGroup();
}

void EntityTree::restore(QString group, QSettings *settings) {
    auto model = table.model();
    auto header = table.header();
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

// bool selectEditColumn(QModelIndex &index) {
//     auto columnCount = index.model()->columnCount();
//     while (index.column() < columnCount) {
//         if ((index.flags() & Qt::ItemIsEditable) && !index.data().isValid()) return true;
//         index = index.sibling(index.row(), index.column()+1);
//     }
//     return false;
// }

void EntityTree::startEdit(int rowIndex) {
    // auto index = sortModel.index(rowIndex, 0);
    // if (selectEditColumn(index)) {
    //     table.selectionModel()->setCurrentIndex(index, QItemSelectionModel::ClearAndSelect);
    //     table.edit(index);
    // }
}

void EntityTree::loadData(QString statusMessage, std::function<void ()> doLoad) {
    if (!dialog::confirmDiscardChanges(window, model)) return;
    table.setEnabled(false); // TODO save/restore selection
    statusBar.addMessage(statusMessage);
    doLoad();
}

void EntityTree::saveData(QString statusMessage, std::function<void ()> doSave) {
    statusBar.addMessage(statusMessage);
    table.setEnabled(false);
    doSave();
}

QAction *EntityTree::addAction(const QString text) {
    auto addAction = finances::iconAction(finances::AddCircle, text, QKeySequence::New, this, SLOT(addRow()));
    connect(&itemDelegate, &TableItemDelegate::openEditor, addAction, [=]() { addAction->setEnabled(false); });
    connect(&itemDelegate, &TableItemDelegate::closeEditor, addAction, [=]() { addAction->setEnabled(true); });
    return addAction;
}

QAction *EntityTree::deleteAction(const QString text, std::function<bool(const QModelIndex &)> enableDelete) {
    auto action = finances::iconAction(finances::Trash, text, QKeySequence::Delete, this, SLOT(queueDeletes()));
    auto setEnabled = [=, this]() {
        auto indexes = sortModel.mapSelectionToSource(table.selectionModel()->selection()).indexes();
        bool enabled = !indexes.empty();
        for (auto i = indexes.cbegin(); enabled && i != indexes.cend(); ++i) enabled &= enableDelete(*i);
        action->setEnabled(enabled);
    };
    setEnabled();
    connect(table.selectionModel(), &QItemSelectionModel::selectionChanged, this, setEnabled);
    return action;
}

QAction *EntityTree::undoAction() {
    auto undoAction = finances::iconAction(finances::Undo, tr("Undo"), QKeySequence::Undo, this, SLOT(undoChanges()));
    return undoAction;
}

void EntityTree::dataChanged() {
    saveAction->setEnabled(model->hasUnsavedChanges() && model->isValid());
    window->setWindowModified(model->hasUnsavedChanges());
}

void EntityTree::addRow() {
    int rowIndex = sortModel.mapFromSource(model->index(model->queueAdd(), 0)).row();
    startEdit(rowIndex);
}

void EntityTree::queueDeletes() {
    auto selection = sortModel.mapSelectionToSource(table.selectionModel()->selection());
    for (auto i : selection.indexes()) model->queueDelete(i);
}

void EntityTree::undoChanges() {
    auto selection = sortModel.mapSelectionToSource(table.selectionModel()->selection());
    for (auto i : selection.indexes()) model->undoChange(i);
}

void EntityTree::showValidation(const QModelIndex &index) {
    // auto range = table.selectionModel()->selection().indexes();
    // TODO index not in range for mouse click
    // qDebug() << "showValidation" << range.length() << range.contains(index);
    // if (range.length() == 1 && range.contains(index)) {
    auto message = index.data(finances::ValidationMessageRole);
    if (!message.isNull()) statusBar.showMessage(message.toString());
    else statusBar.clearMessage();
    // } else statusBar.clearMessage();
}
