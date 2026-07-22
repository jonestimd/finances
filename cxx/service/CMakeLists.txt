qt_add_library(finances-service OBJECT
    ${CMAKE_CURRENT_LIST_DIR}/servicecontext.h ${CMAKE_CURRENT_LIST_DIR}/servicecontext.cpp
    ${CMAKE_CURRENT_LIST_DIR}/accountservice.h ${CMAKE_CURRENT_LIST_DIR}/accountservice.cpp
    ${CMAKE_CURRENT_LIST_DIR}/categoryservice.h ${CMAKE_CURRENT_LIST_DIR}/categoryservice.cpp
    ${CMAKE_CURRENT_LIST_DIR}/companyservice.h ${CMAKE_CURRENT_LIST_DIR}/companyservice.cpp
    ${CMAKE_CURRENT_LIST_DIR}/entityservice.h
    ${CMAKE_CURRENT_LIST_DIR}/groupservice.h ${CMAKE_CURRENT_LIST_DIR}/groupservice.cpp
    ${CMAKE_CURRENT_LIST_DIR}/payeeservice.h ${CMAKE_CURRENT_LIST_DIR}/payeeservice.cpp
    ${CMAKE_CURRENT_LIST_DIR}/securityservice.h ${CMAKE_CURRENT_LIST_DIR}/securityservice.cpp
    ${CMAKE_CURRENT_LIST_DIR}/transactiondetailservice.h ${CMAKE_CURRENT_LIST_DIR}/transactiondetailservice.cpp
    ${CMAKE_CURRENT_LIST_DIR}/transactionservice.h ${CMAKE_CURRENT_LIST_DIR}/transactionservice.cpp
)

add_dependencies(finances-service libdecnumber)

target_link_libraries(finances-service PUBLIC
    Qt${QT_VERSION_MAJOR}::Core
    Qt${QT_VERSION_MAJOR}::Sql
    ${QDECIMAL_BINARY_DIR}/src/libqdecimal.a
    ${QDECIMAL_BINARY_DIR}/decnumber/libdecnumber.a
    ${SQLite3_LIBRARY}
)

target_include_directories(finances-service PRIVATE
    $<TARGET_PROPERTY:finances,INCLUDE_DIRECTORIES>
)

add_subdirectory(model/)
add_subdirectory(database/)

add_subdirectory(tests/)