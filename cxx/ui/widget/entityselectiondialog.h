#ifndef ENTITYSELECTIONDIALOG_H
#define ENTITYSELECTIONDIALOG_H

#include "relationeditor.h"
#include <QDialog>
#include <QLabel>

class EntitySelectionDialog : public QDialog
{
    Q_OBJECT
    RelationEditor *selectionInput;
    const QHash<domain_id, QString> disabledOptions;
    QLabel *errorMessage{new QLabel(this)};
    QPushButton *saveButton;

public:
    EntitySelectionDialog(QWidget *parent, ComboBoxModel *model, const QString &title, const QString &label,
                          QHash<domain_id, QString> disabledOptions = QHash<domain_id, QString>{});

    void setSelectedEntity(const NamedEntity *category);
    optional_id selectedId() const;

private Q_SLOTS:
    void inputChanged();
};

#endif // ENTITYSELECTIONDIALOG_H
