
add_library(sqlite_finances SHARED sqlite_finances.cpp)
add_dependencies(sqlite_finances qdecimal)
target_include_directories(sqlite_finances PRIVATE
    ${QDECIMAL_SOURCE_DIR}/decnumber
    ${SQLite3_INCLUDE_DIR}
)
target_link_libraries(sqlite_finances PUBLIC libdecnumber ${SQLite3_LIBRARY})
target_compile_options(sqlite_finances PRIVATE -x c)
set_target_properties(sqlite_finances PROPERTIES
    PREFIX ""
    AUTOMOC OFF
    LIBRARY_OUTPUT_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
)