#include "accountstore.h"
#include "ui/model/accounttablemodel.h"
#include "ui/widget/statusmessage.h"

Q_STATIC_LOGGING_CATEGORY(logger, "store.account")

AccountStore::AccountStore(ServiceContext *services)
    : EntityStore{&services->accountService}
    , companyStore{&services->companyService} {}

bool AccountStore::load(EntityView *view, bool reload) {
    bool loaded = EntityStore::load(view, QObject::tr(LOADING_ACCOUNTS), reload);
    if (!loaded) companyStore.load(view, QObject::tr(LOADING_COMPANIES), true);
    return loaded;
}

void AccountStore::update(QWidget* source, AccountTableModel* model) {
    update(source, model->unsavedChanges(), model->unsavedAdds(), model->unsavedDeletes());
}

void AccountStore::update(QWidget* source, QList<Account*> updates, const QList<const Account*> adds, const QList<const Account*> deletes) {
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
    if (account) {
        auto name = account->name.toString();
        if (account->companyId.isNull()) return name;
        auto company = companyStore.value(account->companyId.toLongLong());
        if (company) return company->name.toString().append(delimiter).append(name);
        else qCDebug(logger, "qualifiedName: company not loaded: %lld", account->companyId.toLongLong());
    }
    else qCDebug(logger, "qualifiedName: account not loaded: %lld", accountId.toLongLong());
    return accountId.toString();
}

void AccountStore::transactionsUpdated(const QList<TransactionChange> changes) {
    if (updateTransactionCounts(changes, &Transaction::accountId)) {
        emit valuesLoaded(ids());
    }
}
