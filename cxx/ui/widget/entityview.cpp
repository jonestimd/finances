#include "entityview.h"
#include "dialog.h"
#include "tableitemdelegate.h"
#include "entityrowaction.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QTableWidget>
#include <QTimer>

EntityView::EntityView(QWidget *window, AdapterItemModel *model, QAbstractItemView *itemView, QHeaderView *viewHeader,
            StatusBar *statusBar, const QString entityName, const QString defaultSort,
            const char *saveSlot, const char *loadSlot, QList<QAction*> actions)
    : QObject(window)
    , window{window}
    , model{model}
    , sortModel{window}
    , itemView{itemView}
    , viewHeader{viewHeader}
    , filterInput{new FilterInput(tr("%1 filter").arg(entityName), &sortModel, window)}
    , defaultSort{defaultSort}
    , toolbar{window}
    , statusBar{statusBar}
    , itemDelegate{window, statusBar}
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
    toolbar.addAction(new AddRowAction(entityName, &itemDelegate, &sortModel, model, itemView, this));
    toolbar.addAction(new DeleteRowAction(entityName, &sortModel, model, itemView, this));
    toolbar.addAction(new UndoChangeAction(&sortModel, model, itemView, this));
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
    viewHeader->setStretchLastSection(true);
}

void EntityView::setColumnResize(const std::vector<int> stretchColumns) {
    viewHeader->setSectionResizeMode(QHeaderView::ResizeToContents);
    for (int i : stretchColumns) {
        viewHeader->setSectionResizeMode(i, QHeaderView::Stretch);
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
    if (viewHeader->sortIndicatorSection() >= 0) {
        settings->setValue("sort.column", model->headerData(viewHeader->sortIndicatorSection(), Qt::Horizontal));
        settings->setValue("sort.order", viewHeader->sortIndicatorOrder());
    }
}

void EntityView::saveSizes(QString group, QSettings *settings) {
    settings->beginGroup(QString(group).append(".columns"));
    for (int section = 0; section < viewHeader->count(); ++section) {
        auto name = model->headerData(section, Qt::Horizontal).toString();
        auto width = viewHeader->sectionSize(section);
        settings->setValue(name + ".width", width);
        settings->setValue(name + ".pos", viewHeader->visualIndex(section));
    }
    settings->endGroup();
}

void EntityView::restore(QString group, QSettings *settings) {
    auto model = itemView->model();
    auto sortColumn = settings->value(group + "/sort.column", defaultSort).toString();
    if (!sortColumn.isEmpty()) {
        auto sortOrder = settings->value(group + "/sort.order", 0).toInt();
        auto index = columnIndex(sortColumn);
        viewHeader->setSortIndicator(index, static_cast<Qt::SortOrder>(sortOrder));
    }
    for (int section = 0; section < viewHeader->count(); ++section) {
        bool ok;
        auto name = model->headerData(section, Qt::Horizontal, Qt::DisplayRole).toString();
        QString column = group + ".columns/" + name;
        auto width = settings->value(column + ".width").toInt(&ok);
        if (ok) viewHeader->resizeSection(section, width);
        auto pos = settings->value(column + ".pos").toInt(&ok);
        if (ok) viewHeader->moveSection(viewHeader->visualIndex(section), pos);
    }
}

bool EntityView::confirmLoadData(QString loadingMessage) {
    if (dialog::confirmDiscardChanges(window, model)) {
        itemView->setEnabled(false); // TODO save/restore selection
        statusBar->addMessage(loadingMessage);
        return true;
    }
    return false;
}

void EntityView::enableUi() {
    statusBar->clear();
    itemView->setEnabled(true);
}

void EntityView::dataChanged() {
    saveAction->setEnabled(model->hasUnsavedChanges() && model->isValid());
    window->setWindowModified(model->hasUnsavedChanges());
}

void EntityView::showValidation(const QModelIndex &index) {
    // make sure index is in selection
    if (!itemView->selectionModel()->hasSelection()) itemView->selectionModel()->select(index, QItemSelectionModel::Select);
    auto message = index.data(finances::ValidationMessageRole);
    if (!message.isNull()) statusBar->showMessage(message.toString());
    else statusBar->clearMessage();
}

EntityView::EntityView(QWidget *window, AdapterItemModel *model, QTableView *view, StatusBar *statusBar, const QString filterLabel,
                       const QString defaultSort, const char *saveSlot, const char *loadSlot, QList<QAction *> actions)
    : EntityView(window, model, view, view->horizontalHeader(), statusBar, filterLabel, defaultSort, saveSlot, loadSlot, actions)
{
    view->resizeColumnsToContents();
    view->setSortingEnabled(true);
    // view->verticalHeader()->setDefaultSectionSize(5); // minimize row height

    viewHeader->setSectionsMovable(true);
    viewHeader->setSortIndicatorShown(true);
    viewHeader->setSortIndicator(0, Qt::SortOrder::AscendingOrder);
}

EntityView::EntityView(QWidget *window, AdapterItemModel *model, QTreeView *view, StatusBar *statusBar, const QString filterLabel,
                       const QString defaultSort, const char *saveSlot, const char *loadSlot, QList<QAction *> actions)
    : EntityView(window, model, view, view->header(), statusBar, filterLabel, defaultSort, saveSlot, loadSlot, actions)
{
    using enum QAbstractItemView::EditTrigger;
    view->setSortingEnabled(true);
    view->setSelectionBehavior(QAbstractItemView::SelectItems);
    view->setEditTriggers(AllEditTriggers ^ CurrentChanged);

    viewHeader->setSectionsMovable(true);
    viewHeader->setSortIndicatorShown(true);
    viewHeader->setSortIndicator(0, Qt::SortOrder::AscendingOrder);
}
