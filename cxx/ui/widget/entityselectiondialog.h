#ifndef ENTITYSELECTIONDIALOG_H
#define ENTITYSELECTIONDIALOG_H

#include "relationeditor.h"
#include <QDialog>
#include <QLabel>

class EntitySelectionDialog : public QDialog
{
    Q_OBJECT
    RelationEditor *selectionInput;
    const QHash<qlonglong, QString> disabledOptions;
    QLabel *errorMessage{new QLabel(this)};
    QPushButton *saveButton;

public:
    EntitySelectionDialog(QWidget *parent, ComboBoxModel *model, const QString &title, const QString &label,
                          QHash<qlonglong, QString> disabledOptions = QHash<qlonglong, QString>{});

    void setSelectedEntity(const NamedEntity *category);
    std::optional<qlonglong> selectedId() const;
    QVariant qSelectedId() const;

private Q_SLOTS:
    void inputChanged();
};

#endif // ENTITYSELECTIONDIALOG_H
