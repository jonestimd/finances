#include "entityview.h"
#include "dialog.h"
#include "tableitemdelegate.h"
#include "entityrowaction.h"
#include "ui/widget/settings.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QLayout>
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

EntityView::EntityView(QWidget *window, StatusMessageStore* messageStore, AdapterItemModel *model, QAbstractItemView *itemView, QHeaderView *viewHeader, const QString &entityName)
    : QObject(window)
    , window{window}
    , sortModel{window}
    , itemView{itemView}
    , viewHeader{viewHeader}
    , filterInput{new FilterInput(tr("%1 filter").arg(entityName), &sortModel, window)}
    , toolbar{window}
    , itemDelegate{window, &statusBar}
    , saveAction{finances::saveAction(window)}
{
    sortModel.setSourceModel(model);
    sortModel.setSortRole(finances::SortRole);
    sortModel.setFilterKeyColumn(-1);
    sortModel.setSortCaseSensitivity(Qt::CaseInsensitive);

    itemView->setProperty("sortingEnabled", true);
    itemView->setModel(&sortModel);
    itemView->setItemDelegate(&itemDelegate);
    itemView->setAlternatingRowColors(true);

    viewHeader->setSectionsMovable(true);
    viewHeader->setSortIndicatorShown(true);
    viewHeader->setSortIndicator(0, Qt::SortOrder::AscendingOrder);

    toolbar.setMovable(false);
    toolbar.addAction(new AddRowAction(entityName, &itemDelegate, &sortModel, itemView, this));
    toolbar.addAction(new DeleteRowAction(entityName, &sortModel, itemView, this));
    toolbar.addAction(new UndoChangeAction(&sortModel, itemView, this));
    toolbar.addAction(saveAction);
    toolbar.addAction(finances::reloadAction(window));
    toolbar.addWidget(filterInput);

    connect(&sortModel, SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged()));
    connect(&sortModel, SIGNAL(rowsRemoved(QModelIndex,int,int)), this, SLOT(dataChanged()));
    connect(&sortModel, SIGNAL(rowsInserted(QModelIndex,int,int)), this, SLOT(dataChanged()));
    connect(&sortModel, SIGNAL(modelReset()), this, SLOT(dataChanged()));
    connect(itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)), this, SLOT(showValidation(QModelIndex)));
    connect(itemView->itemDelegate(), &TableItemDelegate::closeEditor, this,
            [this]() { showValidation(this->itemView->selectionModel()->currentIndex()); });
    connect(messageStore, SIGNAL(statusMessage(QString)), this, SLOT(showStatusMessage(QString)));
    connect(messageStore, SIGNAL(isReady()), this, SLOT(clearStatusMessage()));

    finances::setColumnResize(viewHeader);
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
        return sortModel.mapToSource(itemView->selectionModel()->selectedIndexes().first());
    }
    return QModelIndex{};
}

bool EntityView::focusFilter(QKeyEvent *event) {
    if (event->matches(QKeySequence::Find) && !filterInput->hasFocus()) {
        filterInput->setFocus();
        return true;
    }
    return false;
}

bool EntityView::confirmLoadData() {
    return dialog::confirmDiscardChanges(window, model());
}

void EntityView::confirmClose(QCloseEvent *event, const char *settingsGroup) {
    if (!dialog::confirmDiscardChanges(window, model())) event->ignore();
    else settings::saveWindowState(settingsGroup, window, model(), viewHeader);
}

void EntityView::showStatusMessage(const QString message) {
    if (itemView->isEnabled()) {
        lastSelection.clear();
        for (auto i = itemView->currentIndex(); i.isValid(); i = i.parent()) lastSelection.insert(0, i.row());
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

void EntityView::restoreSelection() {
    if (!lastSelection.isEmpty()) {
        QModelIndex index;
        for (int row : std::as_const(lastSelection)) {
            int count = sortModel.rowCount(index);
            index = sortModel.index(row < count ? row : count-1, 0, index);
        }
        itemView->setCurrentIndex(index);
    }
}

void EntityView::focusItemView() {
    if (itemView->isEnabled()) itemView->setFocus();
    else itemView->installEventFilter(new ViewFocusFilter);
}

void EntityView::dataChanged() {
    saveAction->setEnabled(model()->hasUnsavedChanges() && model()->isValid());
    window->setWindowModified(model()->hasUnsavedChanges());
}

void EntityView::showValidation(const QModelIndex &index) {
    // make sure index is in selection
    if (!itemView->selectionModel()->hasSelection()) itemView->selectionModel()->select(index, QItemSelectionModel::Select);
    auto message = index.data(finances::ValidationMessageRole);
    if (!message.isNull()) statusBar.showMessage(message.toString());
    else statusBar.clearMessage();
}

EntityView::EntityView(QWidget *window, StatusMessageStore* messageStore, AdapterItemModel *model, QTableView *view, const QString &entityName)
    : EntityView(window, messageStore, model, view, view->horizontalHeader(), entityName)
{
    view->resizeColumnsToContents();
    // view->verticalHeader()->setDefaultSectionSize(5); // minimize row height
}
