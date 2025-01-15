#include "entitystore.h"

EntityStoreSignals::EntityStoreSignals(QObject *parent) : QObject(parent) {}

const QString EntityStoreSignals::user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))};
