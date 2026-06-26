#include "entityselectiondialog.h"

#include <QDialogButtonBox>
#include <QLabel>
#include <QPushButton>
#include <QVBoxLayout>

EntitySelectionDialog::EntitySelectionDialog(QWidget *parent, ComboBoxModel *model, const QString &title, const QString &label,
    QHash<domain_id, QString> disabledOptions)
    : QDialog{parent}
    , disabledOptions{disabledOptions}
{
    setWindowModality(Qt::WindowModal);
    setWindowTitle(title);
    selectionInput = new RelationEditor(model, this);

    auto layout = new QVBoxLayout(this);
    layout->addWidget(new QLabel(label));
    layout->addWidget(selectionInput);
    layout->addWidget(errorMessage);
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

optional_id EntitySelectionDialog::selectedId() const {
    auto entity = selectionInput->entity();
    return entity ? entity->id : optional_id{};
}

void EntitySelectionDialog::inputChanged() {
    auto id = selectedId();
    if (id.has_value() && disabledOptions.contains(*id)) {
        errorMessage->setText(disabledOptions.value(*id));
        saveButton->setEnabled(false);
    } else {
        errorMessage->clear();
        saveButton->setEnabled(selectionInput->hasAcceptableInput());
    }
}
