#ifndef SECURITYTABLEMODEL_H
#define SECURITYTABLEMODEL_H

#include "podtablemodel.h"
#include "securitystore.h"

class SecurityTableModel : public PodTableModel<Security, SecurityStore> {
    Q_OBJECT

public:
    SecurityTableModel(SecurityStore *store);
};

#endif // SECURITYTABLEMODEL_H
