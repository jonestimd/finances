#include "searchdialog.h"

#include <QDialogButtonBox>
#include <QLabel>
#include <QPushButton>

SearchDialog::SearchDialog(QWidget *parent, DataStore* dataStore)
    : QDialog{parent}
    , layout{this}
    , payeeInput{dataStore->payeeStore->newComboBoxModel()}
    , securityInput{dataStore->securityStore->newComboBoxModel()}
    , categoryInput{dataStore->categoryStore->newComboBoxModel()}
{
    setWindowModality(Qt::WindowModal);
    setWindowTitle(tr("Enter search criteria"));

    layout.setLabelAlignment(Qt::AlignRight);
    layout.addRow(tr("Contains &text:"), &searchText);
    layout.addRow(tr("For &payee:"), &payeeInput);
    layout.addRow(tr("For &security:"), &securityInput);
    layout.addRow(tr("For &category:"), &categoryInput);
    auto buttonBox = new QDialogButtonBox{QDialogButtonBox::Ok, this};
    okButton = buttonBox->button(QDialogButtonBox::Ok);
    okButton->setEnabled(false);
    layout.addWidget(buttonBox);

    connect(&searchText, SIGNAL(textChanged(QString)), this, SLOT(searchTextChanged()));
    connect(&payeeInput, SIGNAL(entityChanged(const NamedEntity*)), this, SLOT(payeeChanged(const NamedEntity*)));
    connect(&securityInput, SIGNAL(entityChanged(const NamedEntity*)), this, SLOT(securityChanged(const NamedEntity*)));
    connect(&categoryInput, SIGNAL(entityChanged(const NamedEntity*)), this, SLOT(categoryChanged(const NamedEntity*)));
    connect(buttonBox, SIGNAL(accepted()), this, SLOT(accept()));
}

void SearchDialog::searchTextChanged() {
    criteria.text = searchText.text();
    inputChanged();
}

void SearchDialog::payeeChanged(const NamedEntity *payee) {
    if (payee) criteria.payeeId = payee->id;
    else criteria.payeeId.reset();
    inputChanged();
}

void SearchDialog::securityChanged(const NamedEntity *security) {
    if (security) criteria.securityId = security->id;
    else criteria.securityId.reset();
    inputChanged();
}

void SearchDialog::categoryChanged(const NamedEntity* category) {
    if (category) criteria.categoryId = category->id;
    else criteria.categoryId.reset();
    inputChanged();
}

void SearchDialog::inputChanged() {
    okButton->setEnabled(!criteria.text.isEmpty() || criteria.payeeId.has_value()
        || criteria.securityId.has_value() || criteria.categoryId.has_value());
}
