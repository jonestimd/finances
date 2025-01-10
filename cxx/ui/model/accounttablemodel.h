#ifndef ACCOUNTTABLEMODEL_H
#define ACCOUNTTABLEMODEL_H

#include "datastore.h"
#include "podtablemodel.h"
#include "comboboxmodel.h"
#include "service/model/account.h"
#include <QAbstractTableModel>

class AccountTableModel : public PodTableModel<Account> {
    Q_OBJECT
    using AddCompany = ComboBoxModel::CreateValue;

public:
    explicit AccountTableModel(DataStore *datastore, QObject *parent, AddCompany addCompany = nullptr);

    void companiesLoaded(const QList<qlonglong> companyIds);
};

#endif // ACCOUNTTABLEMODEL_H
