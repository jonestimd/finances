#include "entitystore.h"

AbstractEntityStore::AbstractEntityStore(QObject *parent) : QObject(parent) {}

const QString AbstractEntityStore::user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))};
