#include "companytablemodel.h"
#include "columnadapter.h"
#include "numbercolumnadapter.h"
#include "../validation/unique.h"

#define COMPANY_NAME_COLUMN 0

CompanyTableModel::CompanyTableModel(const CompanyStore *store, QObject *parent)
    : PodTableModel<Company, CompanyStore> {
        store,
        QList<ColumnAdapter<Company>*>{
            new FieldColumnAdapter<Company, QString>(tr("Company Name"), &Company::name, true, new UniqueValidatorFactory(COMPANY_NAME_COLUMN)),
            new NumberColumnAdapter<Company, int>(tr("Accounts"), &Company::accounts),
        },
        parent,
    }
{}
