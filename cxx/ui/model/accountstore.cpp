#include "accountstore.h"
#include "ui/model/accounttablemodel.h"
#include "ui/widget/statusmessage.h"

AccountStore::AccountStore(ServiceContext *services)
    : EntityStore{&services->accountService}
    , companyStore{&services->companyService} {}

bool AccountStore::load(EntityView *view, bool reload) {
    bool loaded = EntityStore::load(view, QObject::tr(LOADING_ACCOUNTS), reload);
    if (!loaded) companyStore.load(view, QObject::tr(LOADING_COMPANIES), true);
    return loaded;
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
        companyStore.update(companies.values());
        emit valuesLoaded(ids());
        emit companyStore.valuesLoaded(companyStore.ids());
    });
}

QString AccountStore::qualifiedName(const QVariant &accountId, QChar delimiter) const {
    auto account = value(accountId.toLongLong());
    auto name = account->name.toString();
    if (account->companyId.isNull()) return name;
    auto company = companyStore.value(account->companyId.toLongLong());
    return company->name.toString().append(delimiter).append(name);
}
