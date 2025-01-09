#ifndef ENTITYSELECTIONDIALOG_H
#define ENTITYSELECTIONDIALOG_H

#include "relationeditor.h"
#include <QDialog>

class EntitySelectionDialog : public QDialog
{
    Q_OBJECT
    RelationEditor *selectionInput;
    QPushButton *saveButton;

public:
    EntitySelectionDialog(QWidget *parent, ComboBoxModel *model, const QString &title, const QString &label);

    void setSelectedEntity(const NamedEntity *category);
    QVariant selectedId() const;

private Q_SLOTS:
    void inputChanged();
};

#endif // ENTITYSELECTIONDIALOG_H
