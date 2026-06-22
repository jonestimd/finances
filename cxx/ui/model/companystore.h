#ifndef COMPANYSTORE_H
#define COMPANYSTORE_H

#include "entitystore.h"
#include "service/companyservice.h"

class AccountStore;

class CompanyStore : public EntityStore<Company, CompanyService> {
    friend AccountStore;

public:
    CompanyStore(CompanyService *service, StatusMessageStore* messageStore);

    void addCompany(QWidget *source, const QString &name, const char *callback);
};

#endif // COMPANYSTORE_H
