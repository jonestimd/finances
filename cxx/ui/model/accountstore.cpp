#include "accountstore.h"
#include "ui/model/accounttablemodel.h"

AccountStore::AccountStore(ServiceContext *services)
    : EntityStore{&services->accountService}
    , companyStore{new CompanyStore(&services->companyService)} {}

AccountStore::~AccountStore() {
    delete companyStore;
}

void AccountStore::update(QWidget *source, AccountTableModel *model) {
    update(source, model->unsavedChanges(), model->unsavedAdds(), model->unsavedDeletes());
}

void AccountStore::update(QWidget *source, QList<Account *> updates, const QList<Account *> adds, const QList<const Account *> deletes) {
    doInBackground(source, [this, updates, adds, deletes] {
        auto changes = BulkUpdate{updates, adds, deletes};
        QHash<qlonglong, const Company*> companies;
        auto accounts = service->update(changes, user, companies);
        update(accounts, deletes);
        companyStore->update(companies.values());
        emit valuesLoaded(ids());
        emit companyStore->valuesLoaded(companyStore->ids());
    });
}
