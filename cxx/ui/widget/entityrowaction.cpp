#include "entityrowaction.h"
#include "ui/finances.h"
#include <QAbstractItemView>

EntityRowAction::EntityRowAction(finances::FontIcon icon, const QString &text, const QKeySequence &shortcut,
    SortFilterProxyModel *sortModel, QObject *parent)
    : QAction{parent}
    , sortModel{sortModel}
{
    finances::initAction(this, icon, text, shortcut);
    connect(this, SIGNAL(triggered(bool)), this, SLOT(doAction()));
}

AdapterItemModel *EntityRowAction::model() const {
    return static_cast<AdapterItemModel*>(sortModel->sourceModel());
}

AddRowAction::AddRowAction(const QString &entityName, TableItemDelegate *itemDelegate,
    SortFilterProxyModel *sortModel, QAbstractItemView *itemView, QObject *parent)
    : EntityRowAction{finances::AddCircle, tr("Add %1").arg(entityName), QKeySequence::New, sortModel, parent}
    , itemDelegate{itemDelegate}
    , itemView{itemView}
{
    connect(itemDelegate, SIGNAL(openEditor(QWidget*)), this, SLOT(openEditor()));
    connect(itemDelegate, SIGNAL(closeEditor(QWidget*,QAbstractItemDelegate::EndEditHint)), this, SLOT(closeEditor()));
}

void AddRowAction::openEditor() {
    setEnabled(false);
}

void AddRowAction::closeEditor() {
    setEnabled(true);
}

inline bool selectEditColumn(QModelIndex &index) {
    auto columnCount = index.model()->columnCount();
    while (index.column() < columnCount) {
        if ((index.flags() & Qt::ItemIsEditable) && !index.data().isValid()) return true;
        index = index.sibling(index.row(), index.column()+1);
    }
    return false;
}

void AddRowAction::doAction() {
    auto selectedIndex = sortModel->mapToSource(itemView->selectionModel()->currentIndex());
    auto rowIndex = model()->queueAdd(selectedIndex);
    if (rowIndex.isValid()) {
        auto index = sortModel->mapFromSource(rowIndex).siblingAtColumn(0);
        if (index.isValid() && selectEditColumn(index)) {
            itemView->selectionModel()->setCurrentIndex(index, QItemSelectionModel::ClearAndSelect);
            itemView->edit(index);
        }
    }
}

DeleteRowAction::DeleteRowAction(const QString &entityName, SortFilterProxyModel *sortModel, QAbstractItemView *itemView, QObject *parent)
    : EntityRowAction{finances::Trash, tr("Delete %1").arg(entityName), QKeySequence::Delete, sortModel, parent}
    , selectionModel{itemView->selectionModel()}
{
    connect(selectionModel, SIGNAL(selectionChanged(QItemSelection,QItemSelection)), this, SLOT(selectionChanged()));
    connect(sortModel, SIGNAL(modelReset()), this, SLOT(selectionChanged()));
    selectionChanged();
}

void DeleteRowAction::selectionChanged() {
    auto indexes = sortModel->mapSelectionToSource(selectionModel->selection()).indexes();
    bool enabled = !indexes.empty();
    for (auto i = indexes.cbegin(); enabled && i != indexes.cend(); ++i) enabled &= model()->enableDelete(*i);
    setEnabled(enabled);
}

void DeleteRowAction::doAction() {
    auto selection = sortModel->mapSelectionToSource(selectionModel->selection()).indexes();
    for (auto i = selection.cbegin(), end = selection.cend(); i != end; i++) model()->queueDelete(*i);
    selectionChanged();
}

UndoChangeAction::UndoChangeAction(SortFilterProxyModel *sortModel, QAbstractItemView *itemView, QObject *parent)
    : EntityRowAction(finances::Undo, tr("Undo"), QKeySequence::Undo, sortModel, parent)
    , selectionModel{itemView->selectionModel()}
{}

void UndoChangeAction::doAction() {
    auto selection = sortModel->mapSelectionToSource(selectionModel->selection()).indexes();
    for (auto i = selection.cbegin(), end = selection.cend(); i != end; i++) model()->undoChange(*i);
}
