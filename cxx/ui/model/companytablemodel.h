#ifndef COMPANYTABLEMODEL_H
#define COMPANYTABLEMODEL_H

#include "service/model/company.h"
#include "podtablemodel.h"

class CompanyTableModel : public PodTableModel<Company> {
    Q_OBJECT
public:
    explicit CompanyTableModel(const EntityStore<const Company*> *store, QObject *parent = nullptr);
};

#endif // COMPANYTABLEMODEL_H
