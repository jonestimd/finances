#include "entityview.h"
#include "dialog.h"
#include "tableitemdelegate.h"
#include "entityrowaction.h"
#include "ui/model/adapteritemmodel.h"
#include "ui/widget/itemview.h"
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
    , sortModel{new SortFilterProxyModel{window}}
    , itemView{itemView}
    , viewHeader{viewHeader}
    , filterInput{new FilterInput(tr("%1 filter").arg(entityName), sortModel, window)}
    , toolbar{window}
{
    sortModel->setSourceModel(model);
    itemview::init(window, sortModel, itemView, viewHeader, &statusBar);

    toolbar.setMovable(false);
    if (this->model<AdapterItemModel>()) {
        auto itemDelegate = static_cast<TableItemDelegate*>(itemView->itemDelegate());
        toolbar.addAction(new AddRowAction(entityName, itemDelegate, sortModel, itemView, this));
        toolbar.addAction(new DeleteRowAction(entityName, sortModel, itemView, this));
    }
    if (this->model<ChangeTrackingItemModel>()) {
        toolbar.addAction(new UndoChangeAction(sortModel, itemView, this));
        toolbar.addAction(finances::saveAction(window, sortModel));
    }
    toolbar.addAction(finances::reloadAction(window));
    toolbar.addWidget(filterInput);

    connect(messageStore, SIGNAL(statusMessage(QString)), this, SLOT(showStatusMessage(QString)));
    connect(messageStore, SIGNAL(isReady()), this, SLOT(clearStatusMessage()));

    window->installEventFilter(this);
    if (this->model<ChangeTrackingItemModel>()) {
        new ChangeHandler{this, sortModel, [window](const ChangeTrackingItemModel* model) {
            window->setWindowModified(model->hasUnsavedChanges());
        }};
        connect(itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)), this, SLOT(showValidation(QModelIndex)));
        connect(itemView->itemDelegate(), &TableItemDelegate::closeEditor, this,
            [this]() { showValidation(this->itemView->selectionModel()->currentIndex()); });
    }
    auto tableView = qobject_cast<QTableView*>(itemView);
    if (tableView) {
        tableView->resizeColumnsToContents();
        // tableView->verticalHeader()->setDefaultSectionSize(5); // minimize row height
    }
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

void EntityView::showValidation(const QModelIndex& index) {
    auto selectionModel = itemView->selectionModel();
    // make sure index is in selection
    if (!selectionModel->hasSelection()) selectionModel->select(index, QItemSelectionModel::Select);
    auto message = index.data(finances::ValidationMessageRole);
    if (!message.isNull()) statusBar.showMessage(message.toString());
    else statusBar.clearMessage();
}

bool EntityView::eventFilter(QObject *obj, QEvent *event) {
    if (event->isInputEvent() && event->type() == QEvent::ShortcutOverride) {
        auto keyEvent = static_cast<QKeyEvent*>(event);
        if (keyEvent && keyEvent->matches(QKeySequence::Find) && !filterInput->hasFocus()) {
            filterInput->setFocus();
            return true;
        }
    } else if (event->type() == QEvent::Close) {
        auto model = this->model<ChangeTrackingItemModel>();
        if (model && !dialog::confirmDiscardChanges(window, model)) {
            event->ignore();
            return true;
        }
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
