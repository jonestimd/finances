#ifndef ACCOUNTTABLEMODEL_H
#define ACCOUNTTABLEMODEL_H

#include "../../service/model/account.h"
#include "../../service/model/company.h"
#include "podtablemodel.h"
#include <QAbstractTableModel>

class AccountTableModel : public PodTableModel<Account> {
    QList<Company*> companies_;
public:
    explicit AccountTableModel(QObject *parent = nullptr);

    const QList<Company*> companies() const;

    void setCompanies(QList<Company*> companies);
};

#endif // ACCOUNTTABLEMODEL_H
