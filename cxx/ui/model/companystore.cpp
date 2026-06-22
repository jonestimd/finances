#include "companystore.h"
#include "ui/widget/statusmessage.h"

CompanyStore::CompanyStore(CompanyService *service, StatusMessageStore* messageStore) : EntityStore{service, messageStore} {}

void CompanyStore::addCompany(QWidget *source, const QString &name, const char *callback) {
    messageStore->addMessage(tr(SAVING_COMPANY));
    doInBackground(source, [=, this] {
        auto company = service->add(name, user);
        update(QList{company});
        emit valuesLoaded(ids());
        QMetaObject::invokeMethod(source, callback, company);
    }, [=, this]() {
        QMetaObject::invokeMethod(source, callback, nullptr);
        QMetaObject::invokeMethod(messageStore, &StatusMessageStore::removeMessage, Qt::QueuedConnection, tr(SAVING_COMPANY));
    });
}
