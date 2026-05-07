/**
 * @brief Sqlite3 extension that provides the `adjust_shares` function used by `finances`.
 */
#include <sqlite3ext.h>
#include <assert.h>
#include <decNumber.h>

SQLITE_EXTENSION_INIT1

static const char* const stockSplitsSql = R"(
select shares_in, shares_out
from stock_split
where security_id = :securityId and date >= :fromDate)";

#define MAXSTRINGSIZE 512

static decContext ctx;

static void adjustShares(sqlite3_context *context, int argc, sqlite3_value **argv) {
    assert(argc == 3);
    for (int i = 0; i < 3; i++) {
        if (sqlite3_value_type(argv[i]) == SQLITE_NULL) {
            sqlite3_result_null(context);
            return;
        }
    }
    sqlite3* db = sqlite3_context_db_handle(context);
    sqlite3_stmt *stmt;
    decNumber shares, sharesIn, sharesOut;
    sqlite_int64 securityId = sqlite3_value_int64(argv[0]);
    const char* fromDate = (const char*)sqlite3_value_text(argv[1]);
    decNumberFromString(&shares, (const char*)sqlite3_value_text(argv[2]), &ctx);

    const char * message = "adjust_shares: fetch row failed";
    int status = sqlite3_prepare_v2(db, stockSplitsSql, -1, &stmt, NULL);
    if (status != SQLITE_OK) message = "adjust_shares: prepare failed";
    else {
        sqlite3_bind_int64(stmt, 1, securityId);
        sqlite3_bind_text(stmt, 2, fromDate, -1, NULL);
        while ((status = sqlite3_step(stmt)) == SQLITE_ROW) {
            decNumberFromString(&sharesIn, (const char*)sqlite3_column_text(stmt, 0), &ctx);
            decNumberFromString(&sharesOut, (const char*)sqlite3_column_text(stmt, 1), &ctx);
            // shares = shares * sharesOut / sharesIn;
            decNumberMultiply(&shares, &shares, &sharesOut, &ctx);
            decNumberDivide(&shares, &shares, &sharesIn, &ctx);
        }
    }
    sqlite3_finalize(stmt);
    if (status == SQLITE_DONE) {
        char result[MAXSTRINGSIZE];
        decNumberToString(&shares, result);
        sqlite3_result_text(context, result, -1, SQLITE_TRANSIENT);
    } else {
        sqlite3_result_error(context, message, -1);
    }
}

#ifdef _WIN32
__declspec(dllexport)
#endif
int sqlite3_finances_init(sqlite3 *db, char **pzErrMsg, const sqlite3_api_routines *pApi) {
    SQLITE_EXTENSION_INIT2(pApi);
    decContextDefault(&ctx, DEC_INIT_BASE);
    return pApi->create_function_v2(db, "adjust_shares", 3, SQLITE_UTF8, NULL, adjustShares, NULL, NULL, NULL);
}
