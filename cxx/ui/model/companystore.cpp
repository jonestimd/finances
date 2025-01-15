#include "companystore.h"

CompanyStore::CompanyStore(CompanyService *service) : EntityStore{service} {}

void CompanyStore::addCompany(QWidget *source, const QString &name, const char *callback) {
    doInBackground(source, [=, this] {
        auto company = service->add(name, user);
        update(QList{company});
        emit valuesLoaded(ids());
        QMetaObject::invokeMethod(source, callback, company);
    }, [=]() {
        QMetaObject::invokeMethod(source, callback, nullptr);
    });
}
