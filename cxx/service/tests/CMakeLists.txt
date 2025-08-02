get_target_property(DATABASE_SOURCES finances SOURCES)
list(FILTER DATABASE_SOURCES INCLUDE REGEX ".*/database/.*")

get_target_property(MODEL_SOURCES finances SOURCES)
list(FILTER MODEL_SOURCES INCLUDE REGEX ".*/model/.*")
message(NOTICE ${MODEL_SOURCES})

qt_add_library(dbtestcase OBJECT
    dbtestcase.h dbtestcase.cpp
    ${DATABASE_SOURCES}
    ${MODEL_SOURCES}
)

target_link_libraries(dbtestcase PRIVATE
    Qt${QT_VERSION_MAJOR}::Core
    Qt${QT_VERSION_MAJOR}::Sql
    ${BINARY_DIR}/src/libqdecimal.a
    ${BINARY_DIR}/decnumber/libdecnumber.a
)

target_include_directories(dbtestcase PRIVATE
    ${SOURCE_DIR}/src
    ${SOURCE_DIR}/decnumber
    ${CMAKE_HOME_DIRECTORY}/cxx
)

qt_add_executable(test_transactionservice test_transactionservice.cpp ../transactionservice.cpp)

add_dependencies(test_transactionservice qdecimal dbtestcase)

set_target_properties(test_transactionservice PROPERTIES
    AUTOMOC ON
    WIN32_EXECUTABLE TRUE
    MACOSX_BUNDLE TRUE
)

target_include_directories(test_transactionservice PRIVATE
    ${SOURCE_DIR}/src
    ${SOURCE_DIR}/decnumber
    ${CMAKE_HOME_DIRECTORY}/cxx
)

target_link_libraries(test_transactionservice PRIVATE
    Qt${QT_VERSION_MAJOR}::Core
    Qt${QT_VERSION_MAJOR}::Sql
    Qt${QT_VERSION_MAJOR}::Test
    ${BINARY_DIR}/src/libqdecimal.a
    ${BINARY_DIR}/decnumber/libdecnumber.a
    dbtestcase
)

install(TARGETS test_transactionservice
    BUNDLE  DESTINATION .
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

qt_generate_deploy_app_script(
    TARGET test_transactionservice
    OUTPUT_SCRIPT deploy_test_transactionservice
    NO_UNSUPPORTED_PLATFORM_ERROR
)
install(SCRIPT ${deploy_test_transactionservice})

add_test(NAME test_transactionservice COMMAND test_transactionservice)
