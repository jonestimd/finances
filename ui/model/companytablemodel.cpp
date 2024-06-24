#include "companytablemodel.h"
#include "columnadapter.h"
#include "numbercolumnadapter.h"

CompanyTableModel::CompanyTableModel(QList<Company*> companies, QObject *parent)
    : PodTableModel<Company>{
        QList<ColumnAdapter<Company>*>{
            new ColumnAdapter<Company>(tr("Company Name"), &Company::name),
            new NumberColumnAdapter<Company>(tr("Accounts"), &Company::accounts),
        },
        parent,
    }
{
    setRows(companies);
}
