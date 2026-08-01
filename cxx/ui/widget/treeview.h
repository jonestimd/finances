#ifndef TREEVIEW_H
#define TREEVIEW_H

#include <QTreeView>

class TreeView : public QTreeView {
    Q_OBJECT
    bool childInheritsBackground_{false};
    QMetaObject::Connection modelResetConnection;
    QMetaObject::Connection rowsInsertedConnection;

public:
    TreeView();

    bool childInheritsBackground() const;
    void setChildInheritsBackground(bool value);

    void setRootSpansAllColumns();

    void setModel(QAbstractItemModel *model) override;

private Q_SLOTS:
    void modelReset();
    void rowsInserted(QModelIndex parent, int first, int last);

protected:
    void drawRow(QPainter *painter, const QStyleOptionViewItem &options, const QModelIndex &index) const override;

    QModelIndex moveCursor(CursorAction cursorAction, Qt::KeyboardModifiers modifiers) override;

private:
    bool isHidden(const QModelIndex &index) const;
    QModelIndex nextCell(const QModelIndex &index) const;
    QModelIndex previousCell(const QModelIndex &index) const;
    QModelIndex nextColumn(QModelIndex index) const;
    QModelIndex previousColumn(QModelIndex index) const;
};

#endif // TREEVIEW_H
