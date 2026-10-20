function(add_sqlite_lib name type)
    add_library(${name} ${type}
        ${CMAKE_CURRENT_LIST_DIR}/sqlite_finances.h ${CMAKE_CURRENT_LIST_DIR}/sqlite_finances.cpp
    )
    add_dependencies(${name} qdecimal sqlite3)
    target_include_directories(${name} PRIVATE
        ${QDECIMAL_SOURCE_DIR}/decnumber
        ${SQLite3_INCLUDE_DIR}
    )
    target_link_libraries(${name} PUBLIC libdecnumber ${SQLite3_LIBRARY})
    target_compile_options(${name} PRIVATE -x c)
    if(type EQUAL "SHARED")
        target_compile_definitions(${name} PRIVATE NOSQLITE_CORE)
    endif()

    set_target_properties(${name} PROPERTIES
        PREFIX ""
        AUTOMOC OFF
    )
endFunction()

add_sqlite_lib(sqlite_finances_ext SHARED)
add_sqlite_lib(sqlite_finances OBJECT)
