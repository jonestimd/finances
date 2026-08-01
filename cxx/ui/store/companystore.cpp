#include "companystore.h"
#include "ui/widget/statusmessage.h"

CompanyStore::CompanyStore(CompanyService *service, StatusMessageStore* messageStore) : EntityStore{service, messageStore} {}

void CompanyStore::addCompany(QWidget *source, const QString &name, const char *callback) {
    doInBackground(source, tr(SAVING_COMPANY), [=, this] {
        auto company = service->add(name, user);
        update(QList{company});
        emit valuesLoaded(ids());
        QMetaObject::invokeMethod(source, callback, company);
    }, [=, this]() {
        QMetaObject::invokeMethod(source, callback, nullptr);
    });
}
