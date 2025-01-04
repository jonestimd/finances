#include "companytablemodel.h"
#include "columnadapter.h"
#include "numbercolumnadapter.h"
#include "../validation/unique.h"

#define COMPANY_NAME_COLUMN 0

CompanyTableModel::CompanyTableModel(const EntityStore<const Company*> *store, QObject *parent)
    : PodTableModel<Company> {
        store,
        QList<ColumnAdapter<Company>*>{
            new ColumnAdapter<Company>(tr("Company Name"), &Company::name, true, new UniqueValidatorFactory(COMPANY_NAME_COLUMN)),
            new NumberColumnAdapter<Company>(tr("Accounts"), &Company::accounts),
        },
        parent,
    }
{}
