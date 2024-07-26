#ifndef ACCOUNTTABLEMODEL_H
#define ACCOUNTTABLEMODEL_H

#include "datastore.h"
#include "podtablemodel.h"
#include "comboboxmodel.h"
#include "service/model/account.h"
#include <QAbstractTableModel>

class AccountTableModel : public PodTableModel<Account> {
    using AddCompany = ComboBoxModel::CreateValue;

    DataStore *dataStore;
public:
    explicit AccountTableModel(DataStore *datastore, QObject *parent, AddCompany addCompany = nullptr);
};

#endif // ACCOUNTTABLEMODEL_H
