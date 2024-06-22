#ifndef ACCOUNTTABLEMODEL_H
#define ACCOUNTTABLEMODEL_H

#include "../../service/model/account.h"
#include "../../service/model/company.h"
#include "podtablemodel.h"
#include <QAbstractTableModel>

class AccountTableModel : public PodTableModel<Account> {
    QList<Company*> companies;
public:
    explicit AccountTableModel(QObject *parent = nullptr);

    void setCompanies(QList<Company*>);
};

#endif // ACCOUNTTABLEMODEL_H
