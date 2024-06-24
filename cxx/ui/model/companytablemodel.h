#ifndef COMPANYTABLEMODEL_H
#define COMPANYTABLEMODEL_H

#include "../../service/model/company.h"
#include "podtablemodel.h"

class CompanyTableModel : public PodTableModel<Company> {
public:
    explicit CompanyTableModel(QList<Company*> companies, QObject *parent = nullptr);
};

#endif // COMPANYTABLEMODEL_H
