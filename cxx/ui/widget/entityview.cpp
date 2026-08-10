#include "entityview.h"
#include "dialog.h"
#include "tableitemdelegate.h"
#include "entityrowaction.h"
#include "ui/widget/settings.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QLayout>
#include <QMainWindow>
#include <QTableWidget>
#include <QTimer>

class ViewFocusFilter : public QObject {
public:
    virtual bool eventFilter(QObject *watched, QEvent *event) override {
        if (event->type() == QEvent::EnabledChange) {
            auto widget = qobject_cast<QWidget*>(watched);
            widget->removeEventFilter(this);
            widget->setFocus();
            deleteLater();
        }
        return false;
    }
};

/////////////// EntityView ///////////////

EntityView::EntityView(QWidget *window, StatusMessageStore *messageStore, QAbstractItemModel *model, QAbstractItemView *itemView,
                       QHeaderView *viewHeader, const QString &entityName)
    : QObject(window)
    , window{window}
    , sortModel{new SortFilterProxyModel(window)}
    , itemView{itemView}
    , viewHeader{viewHeader}
    , filterInput{new FilterInput(tr("%1 filter").arg(entityName), sortModel, window)}
    , toolbar{window}
    , itemDelegate{window, &statusBar}
{
    setModel(model);
    sortModel->setSortRole(finances::SortRole);
    sortModel->setFilterKeyColumn(-1);
    sortModel->setSortCaseSensitivity(Qt::CaseInsensitive);

    itemView->setProperty("sortingEnabled", true);
    itemView->setModel(sortModel);
    itemView->setItemDelegate(&itemDelegate);
    itemView->setAlternatingRowColors(true);

    viewHeader->setSectionsMovable(true);
    viewHeader->setSortIndicatorShown(true);
    viewHeader->setSortIndicator(0, Qt::SortOrder::AscendingOrder);

    toolbar.setMovable(false);
    toolbar.addAction(finances::reloadAction(window));
    toolbar.addWidget(filterInput);

    connect(messageStore, SIGNAL(statusMessage(QString)), this, SLOT(showStatusMessage(QString)));
    connect(messageStore, SIGNAL(isReady()), this, SLOT(clearStatusMessage()));

    finances::setColumnResize(viewHeader);
    window->installEventFilter(this);
    auto tableView = qobject_cast<QTableView*>(itemView);
    if (tableView) {
        tableView->resizeColumnsToContents();
        // tableView->verticalHeader()->setDefaultSectionSize(5); // minimize row height
    }
}

void EntityView::setModel(QAbstractItemModel *model) {
    sortModel->setSourceModel(model);
}

void EntityView::addActions(const QList<QAction *> &actions) {
    if (!actions.isEmpty()) {
        auto filterAction = toolbar.actions().constLast();
        toolbar.insertSeparator(filterAction);
        for (auto action : actions) {
            toolbar.insertAction(filterAction, action);
        }
    }
}

void EntityView::insertAction(qsizetype index, QAction *action) {
    toolbar.insertAction(toolbar.actions().at(index), action);
}

QModelIndex EntityView::selectedIndex() {
    if (itemView->selectionModel()->hasSelection()) {
        return sortModel->mapToSource(itemView->selectionModel()->selectedIndexes().first());
    }
    return QModelIndex{};
}

void EntityView::focusItemView() {
    if (itemView->isEnabled()) itemView->setFocus();
    else itemView->installEventFilter(new ViewFocusFilter);
}

void EntityView::showStatusMessage(const QString message) {
    if (itemView->isEnabled()) {
        lastSelection.clear();
        auto index = itemView->currentIndex();
        lastColumn = index.column();
        for (; index.isValid(); index = index.parent()) lastSelection.insert(0, index.row());
        itemView->setEnabled(false);
    }
    statusBar.showMessage(message);
}

void EntityView::clearStatusMessage() {
    statusBar.clearMessage();
    statusBar.showMessage(tr("Ready"), 1500);
    itemView->setEnabled(true);
    itemView->setFocus();
    restoreSelection();
}

bool EntityView::eventFilter(QObject *obj, QEvent *event) {
    if (event->isInputEvent() && event->type() == QEvent::ShortcutOverride) {
        auto keyEvent = static_cast<QKeyEvent*>(event);
        if (keyEvent && keyEvent->matches(QKeySequence::Find) && !filterInput->hasFocus()) {
            filterInput->setFocus();
            return true;
        }
    } else if (event->type() == QEvent::Close) {
        auto settingsGroup = window->property(SETTINGS_GROUP_PROP);
        if (settingsGroup.isValid()) settings::saveWindowState(settingsGroup.toString(), window, sortModel->sourceModel(), viewHeader);
    }
    return false;
}

void EntityView::restoreSelection() {
    if (!lastSelection.isEmpty()) {
        QModelIndex index{};
        for (int row : std::as_const(lastSelection)) {
            int count = sortModel->rowCount(index);
            index = sortModel->index(std::min(row, count-1), 0, index);
        }
        int column = std::min(lastColumn, sortModel->columnCount({})-1);
        itemView->setCurrentIndex(index.siblingAtColumn(column));
        lastSelection.clear();
    }
}

/////////////// EditEntityView ///////////////

EditEntityView::EditEntityView(QWidget *window, StatusMessageStore* messageStore, AdapterItemModel *model,
                       QAbstractItemView *itemView, QHeaderView *viewHeader, const QString &entityName)
    : EntityView{window, messageStore, model, itemView, viewHeader, entityName}
    , saveAction{finances::saveAction(window)}
{
    setModel(model);

    auto firstAction = toolbar.actions().constFirst();
    toolbar.insertAction(firstAction, new AddRowAction(entityName, &itemDelegate, sortModel, itemView, this));
    toolbar.insertAction(firstAction, new DeleteRowAction(entityName, sortModel, itemView, this));
    toolbar.insertAction(firstAction, new UndoChangeAction(sortModel, itemView, this));
    toolbar.insertAction(firstAction, saveAction);

    connect(itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)), this, SLOT(showValidation(QModelIndex)));
    connect(itemView->itemDelegate(), &TableItemDelegate::closeEditor, this,
            [this]() { showValidation(this->itemView->selectionModel()->currentIndex()); });
}

EditEntityView::EditEntityView(QWidget *window, StatusMessageStore* messageStore, AdapterItemModel *model,
                       QTableView *view, const QString &entityName)
    : EditEntityView(window, messageStore, model, view, view->horizontalHeader(), entityName)
{}

void EditEntityView::setModel(AdapterItemModel *model) {
    // need to connect to the source model because some signals are emitted by the proxy model before
    // the source model has completed the change.
    auto oldModel = sortModel->sourceModel();
    if (oldModel) {
        disconnect(oldModel, SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged()));
        disconnect(oldModel, SIGNAL(rowsRemoved(QModelIndex,int,int)), this, SLOT(dataChanged()));
        disconnect(oldModel, SIGNAL(rowsInserted(QModelIndex,int,int)), this, SLOT(dataChanged()));
        disconnect(oldModel, SIGNAL(modelReset()), this, SLOT(dataChanged()));
    }
    EntityView::setModel(model);
    connect(model, SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged()));
    connect(model, SIGNAL(rowsRemoved(QModelIndex,int,int)), this, SLOT(dataChanged()));
    connect(model, SIGNAL(rowsInserted(QModelIndex,int,int)), this, SLOT(dataChanged()));
    connect(model, SIGNAL(modelReset()), this, SLOT(dataChanged()));
}

bool EditEntityView::confirmLoadData() {
    return dialog::confirmDiscardChanges(window, model());
}

void EditEntityView::confirmClose(QCloseEvent *event, const char *settingsGroup) {
    if (!dialog::confirmDiscardChanges(window, model())) event->ignore();
}

void EditEntityView::dataChanged() {
    auto model = this->model();
    saveAction->setEnabled(model->hasUnsavedChanges() && model->isValid());
    window->setWindowModified(model->hasUnsavedChanges());
}

void EditEntityView::showValidation(const QModelIndex &index) {
    // make sure index is in selection
    if (!itemView->selectionModel()->hasSelection()) itemView->selectionModel()->select(index, QItemSelectionModel::Select);
    auto message = index.data(finances::ValidationMessageRole);
    if (!message.isNull()) statusBar.showMessage(message.toString());
    else statusBar.clearMessage();
}

bool EditEntityView::eventFilter(QObject *obj, QEvent *event) {
    if (event->type() == QEvent::Close) {
        confirmClose(static_cast<QCloseEvent*>(event), nullptr);
        return !event->isAccepted() || EntityView::eventFilter(obj, event);
    }
    return EntityView::eventFilter(obj, event);
}
