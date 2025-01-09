#include "entityselectiondialog.h"

#include <QDialogButtonBox>
#include <QLabel>
#include <QPushButton>
#include <QVBoxLayout>

EntitySelectionDialog::EntitySelectionDialog(QWidget *parent, ComboBoxModel *model, const QString &title, const QString &label)
    : QDialog{parent}
{
    setWindowModality(Qt::WindowModal);
    setWindowTitle(title);
    selectionInput = new RelationEditor(model, this);

    auto layout = new QVBoxLayout(this);
    layout->addWidget(new QLabel(label));
    layout->addWidget(selectionInput);
    layout->addStretch();

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Save | QDialogButtonBox::Cancel, this);
    layout->addWidget(buttonBox);
    connect(buttonBox, SIGNAL(accepted()), this, SLOT(accept()));
    connect(buttonBox, SIGNAL(rejected()), this, SLOT(reject()));
    saveButton = buttonBox->button(QDialogButtonBox::Save);
    connect(selectionInput, SIGNAL(textChanged(QString)), this, SLOT(inputChanged()));
}

void EntitySelectionDialog::setSelectedEntity(const NamedEntity *entity) {
    selectionInput->setEntity(entity);
}

QVariant EntitySelectionDialog::selectedId() const {
    auto entity = selectionInput->entity();
    return entity ? entity->id : QVariant{};
}

void EntitySelectionDialog::inputChanged() {
    saveButton->setEnabled(selectionInput->hasAcceptableInput());
}
