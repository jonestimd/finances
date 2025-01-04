#include "categoryparentdialog.h"

#include <QDialogButtonBox>
#include <QLabel>
#include <QPushButton>
#include <QVBoxLayout>

CategoryParentDialog::CategoryParentDialog(QWidget *parent, const CategoryStore *store, const Category *category)
    : QDialog{parent}
    , category{category}
{
    setWindowModality(Qt::WindowModal);
    QList<const NamedEntity*> options;
    for (auto id : store->ids()) {
        auto option = store->value(id);
        if (option != category && !store->isAncestor(id, category->id)) options.append(option);
    }
    auto model = new ComboBoxModel(options, [store](const NamedEntity *category) { return store->displayName(category->id.toLongLong()); });
    parentInput = new RelationEditor(model, this);
    if (!category->parentId.isNull()) {
        parentInput->setEntity(store->value(category->parentId.toLongLong()));
    }

    auto layout = new QVBoxLayout(this);
    layout->addWidget(new QLabel(tr("Select parent category:")));
    layout->addWidget(parentInput);
    layout->addStretch();

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Save | QDialogButtonBox::Cancel, this);
    layout->addWidget(buttonBox);
    connect(buttonBox, SIGNAL(accepted()), this, SLOT(accept()));
    connect(buttonBox, SIGNAL(rejected()), this, SLOT(reject()));
    saveButton = buttonBox->button(QDialogButtonBox::Save);
    connect(parentInput, SIGNAL(textChanged(QString)), this, SLOT(inputChanged()));
}

QVariant CategoryParentDialog::parentId() const {
    auto entity = parentInput->entity();
    return entity ? entity->id : QVariant{};
}

void CategoryParentDialog::inputChanged() {
    saveButton->setEnabled(parentInput->hasAcceptableInput());
}
