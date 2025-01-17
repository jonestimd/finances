#ifndef SECURITYTABLEMODEL_H
#define SECURITYTABLEMODEL_H

#include "podtablemodel.h"
#include "securitystore.h"
#include "service/securityservice.h"

class SecurityTableModel : public PodTableModel<Security, SecurityService> {
    Q_OBJECT

public:
    SecurityTableModel(SecurityStore *store, QObject *parent);
};

#endif // SECURITYTABLEMODEL_H
