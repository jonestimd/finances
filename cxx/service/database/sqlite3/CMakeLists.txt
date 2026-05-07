
add_library(sqlite_finances SHARED sqlite_finances.cpp)
add_dependencies(sqlite_finances qdecimal sqlite3)
target_include_directories(sqlite_finances PRIVATE
    ${SOURCE_DIR}/decnumber
    ${SQLITE_EXTENSION_DIR}
)
target_link_libraries(sqlite_finances PUBLIC libdecnumber)
target_compile_options(sqlite_finances PRIVATE -x c)
set_target_properties(sqlite_finances PROPERTIES
    PREFIX ""
    AUTOMOC OFF
    LIBRARY_OUTPUT_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
)