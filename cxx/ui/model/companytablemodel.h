#ifndef COMPANYTABLEMODEL_H
#define COMPANYTABLEMODEL_H

#include "service/model/company.h"
#include "service/companyservice.h"
#include "podtablemodel.h"
#include "ui/model/datastore.h"

class CompanyTableModel : public PodTableModel<Company, CompanyService> {
    Q_OBJECT
public:
    explicit CompanyTableModel(const CompanyStore *store, QObject *parent = nullptr);
};

#endif // COMPANYTABLEMODEL_H
