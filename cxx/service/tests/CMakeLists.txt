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

function(add_service_test name)
    set(exec_name "test_${name}")
    qt_add_executable(${exec_name} ${exec_name}.cpp ../${name}.cpp ${ARGN})
    add_dependencies(${exec_name} qdecimal dbtestcase)

    set_target_properties(${exec_name} PROPERTIES
        AUTOMOC ON
        WIN32_EXECUTABLE TRUE
        MACOSX_BUNDLE TRUE
    )

    target_include_directories(${exec_name} PRIVATE
        ${SOURCE_DIR}/src
        ${SOURCE_DIR}/decnumber
        ${CMAKE_HOME_DIRECTORY}/cxx
    )

    target_link_libraries(${exec_name} PRIVATE
        Qt${QT_VERSION_MAJOR}::Core
        Qt${QT_VERSION_MAJOR}::Sql
        Qt${QT_VERSION_MAJOR}::Test
        ${BINARY_DIR}/src/libqdecimal.a
        ${BINARY_DIR}/decnumber/libdecnumber.a
        dbtestcase
    )

    # install(TARGETS ${exec_name}
    #     BUNDLE  DESTINATION .
    #     RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    #     LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    # )

    # qt_generate_deploy_app_script(
    #     TARGET ${exec_name}
    #     OUTPUT_SCRIPT deploy_${exec_name}
    #     NO_UNSUPPORTED_PLATFORM_ERROR
    # )
    # install(SCRIPT ${deploy_${exec_name}})

    add_test(NAME ${exec_name} COMMAND ${exec_name})
endfunction()

add_service_test(transactionservice)
add_service_test(categoryservice)
