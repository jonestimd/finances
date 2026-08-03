#ifndef SECURITYTABLEMODEL_H
#define SECURITYTABLEMODEL_H

#include "podtablemodel.h"
#include "ui/store/securitystore.h"

class SecurityTableModel : public PodTableModel<Security, SecurityStore> {
    Q_OBJECT
    const int sharesColumn;

public:
    SecurityTableModel(SecurityStore *store);

private Q_SLOTS:
    void splitsLoaded();
};

#endif // SECURITYTABLEMODEL_H
