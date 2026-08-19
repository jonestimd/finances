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

    layout.addWidget(new QLabel{tr("Contains text:")}, Qt::AlignLeading);
    layout.addWidget(&searchText);
    layout.addWidget(new QLabel{tr("For payee:")}, Qt::AlignLeading);
    layout.addWidget(&payeeInput);
    layout.addWidget(new QLabel{tr("For security:")}, Qt::AlignLeading);
    layout.addWidget(&securityInput);
    layout.addWidget(new QLabel{tr("For category:")}, Qt::AlignLeading);
    layout.addWidget(&categoryInput);
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
