#ifndef ENTITYROWACTION_H
#define ENTITYROWACTION_H

#include "tableitemdelegate.h"
#include "ui/finances.h"
#include "ui/model/adapteritemmodel.h"
#include "ui/model/sortfilterproxymodel.h"
#include <QAction>
#include <QItemSelectionModel>

class EntityRowAction : public QAction {
    Q_OBJECT
protected:
    SortFilterProxyModel *const sortModel;
    AdapterItemModel *const model;

public:
    explicit EntityRowAction(finances::FontIcon icon, const QString &text, const QKeySequence &shortcut,
                             SortFilterProxyModel *sortModel, AdapterItemModel *model, QObject *parent);

protected Q_SLOTS:
    virtual void doAction() = 0;
};

class AddRowAction : public EntityRowAction {
    Q_OBJECT
    TableItemDelegate *const itemDelegate;
    QAbstractItemView *const itemView;

public:
    explicit AddRowAction(const QString &entityName, TableItemDelegate *itemDelegate, SortFilterProxyModel *sortModel,
                          AdapterItemModel *model, QAbstractItemView *itemView, QObject *parent);

private Q_SLOTS:
    void openEditor();
    void closeEditor();

protected:
    virtual void doAction() override;
};

class DeleteRowAction : public EntityRowAction {
    Q_OBJECT
    QItemSelectionModel *const selectionModel;

public:
    explicit DeleteRowAction(const QString &entityName, SortFilterProxyModel *sortModel, AdapterItemModel *model,
                             QAbstractItemView *itemView, QObject *parent);

private Q_SLOTS:
    void selectionChanged();

protected:
    virtual void doAction() override;
};

class UndoChangeAction : public EntityRowAction {
    Q_OBJECT
    QItemSelectionModel *const selectionModel;

public:
    explicit UndoChangeAction(SortFilterProxyModel *sortModel, AdapterItemModel *model, QAbstractItemView *itemView, QObject *parent);

protected:
    virtual void doAction() override;
};

#endif // ENTITYROWACTION_H
