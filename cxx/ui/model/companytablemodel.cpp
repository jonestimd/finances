#include "companytablemodel.h"
#include "columnadapter.h"
#include "numbercolumnadapter.h"
#include "../validation/composite.h"

CompanyTableModel::CompanyTableModel(QList<const Company*> companies, QObject *parent)
    : PodTableModel<Company> {
        QList<ColumnAdapter<Company>*>{
            new ColumnAdapter<Company>(tr("Company Name"), &Company::name, true, requiredUniqueFactory),
            new NumberColumnAdapter<Company>(tr("Accounts"), &Company::accounts),
        },
        parent,
    }
{
    setRows(companies);
}

#include "companytablemodel.moc"
