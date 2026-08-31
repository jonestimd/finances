#ifndef SECURITYLOT_H
#define SECURITYLOT_H

#include "QDecNumber.hh"
#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

/**
 * @brief `SecurityLot` assigns shares from a security purchase to a security sale.
 * @details Security lots are recorded for sales and transfers.  For a sale, the lot links
 * the original purchase (non-transfer) to the sale to indicate how many shares are included
 * in the sale.  For a transfer, the lot links to the original purchase to indicate how many
 * shares have been moved to the new account.
 */
class SecurityLot : public BaseDomain {
public:
    /** @brief Amount of purchase shares assigned to the sale/transfer (not adjusted for splits). */
    QDecNumber purchaseShares;
    /** @brief Amount of sale/transfer shares included in the lot (adjusted for splits as the sale/transfer date). */
    QDecNumber adjustedShares;
    /** @brief ID of the original purchase (never a transfer). */
    domain_id purchaseDetailId;
    /** @brief ID of the sale or transfer. */
    domain_id relatedDetailId;

    SecurityLot();
    SecurityLot(const QSqlRecord &record);
};

#endif // SECURITYLOT_H
