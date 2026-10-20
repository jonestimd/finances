#ifndef SQLITE_FINANCES_H
#define SQLITE_FINANCES_H

#ifdef NOSQLITE_CORE
#undef SQLITE_CORE
#endif

#include <sqlite3ext.h>

#ifdef __cplusplus
extern "C" {
#endif

int sqlite3_finances_init(sqlite3 *db, char **pzErrMsg, const sqlite3_api_routines *pApi);

#ifdef __cplusplus
}
#endif

#endif // SQLITE_FINANCES_H