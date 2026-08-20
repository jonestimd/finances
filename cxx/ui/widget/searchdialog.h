#ifndef SEARCH_DIALOG_H
#define SEARCH_DIALOG_H

#include "service/model/transactiondetail.h"
#include "relationeditor.h"
#include "ui/store/datastore.h"
#include <QDialog>
#include <QFormLayout>
#include <QLineEdit>

class SearchDialog : public QDialog {
    Q_OBJECT
    QFormLayout layout;
    QLineEdit searchText;
    RelationEditor payeeInput;
    RelationEditor securityInput;
    RelationEditor categoryInput;
    QPushButton* okButton;

public:
    DetailSearchCriteria criteria;

    SearchDialog(QWidget* parent, DataStore *dataStore);

private slots:
    void searchTextChanged();
    void payeeChanged(const NamedEntity* payee);
    void securityChanged(const NamedEntity* payee);
    void categoryChanged(const NamedEntity* category);

private:
    void inputChanged();
};

#endif // SEARCH_DIALOG_H