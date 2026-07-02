#ifndef COMPANYTABLEMODEL_H
#define COMPANYTABLEMODEL_H

#include "service/model/company.h"
#include "podtablemodel.h"
#include "ui/model/companystore.h"

class CompanyTableModel : public PodTableModel<Company, CompanyStore> {
    Q_OBJECT
public:
    explicit CompanyTableModel(const CompanyStore *store, QObject *parent = nullptr);
};

#endif // COMPANYTABLEMODEL_H
