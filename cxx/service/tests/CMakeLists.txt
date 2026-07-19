get_target_property(DB_TEST_SOURCES finances SOURCES)
list(FILTER DB_TEST_SOURCES INCLUDE REGEX ".*/service/.*")

qt_add_library(dbtestcase OBJECT
    dbtestcase.h dbtestcase.cpp
    ${DB_TEST_SOURCES}
)

set_target_properties(dbtestcase PROPERTIES
    COMPILE_DEFINITIONS __FINANCES_TEST__
)

target_link_libraries(dbtestcase
    Qt${QT_VERSION_MAJOR}::Core
    Qt${QT_VERSION_MAJOR}::Sql
    ${QDECIMAL_BINARY_DIR}/src/libqdecimal.a
    ${QDECIMAL_BINARY_DIR}/decnumber/libdecnumber.a
    ${SQLite3_LIBRARY}
    $<TARGET_OBJECTS:qtcommon>
)

target_include_directories(dbtestcase PRIVATE
    $<TARGET_PROPERTY:finances,INCLUDE_DIRECTORIES>
)

function(add_service_test name)
    set(exec_name "test_${name}")
    qt_add_executable(${exec_name} ${exec_name}.cpp ${ARGN})
    add_dependencies(${exec_name} qdecimal dbtestcase decimal.so)
    set_target_properties(${exec_name} PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
    )

    set_target_properties(${exec_name} PROPERTIES
        AUTOMOC ON
        WIN32_EXECUTABLE TRUE
        MACOSX_BUNDLE TRUE
    )

    target_include_directories(${exec_name} PRIVATE
        $<TARGET_PROPERTY:finances,INCLUDE_DIRECTORIES>
    )

    target_link_libraries(${exec_name} PRIVATE
        Qt${QT_VERSION_MAJOR}::Test
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

    add_test(NAME ${exec_name}
        WORKING_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
        COMMAND ${exec_name}
    )
endfunction()

add_service_test(transactionservice)
add_service_test(categoryservice)
add_service_test(accountservice)
add_service_test(securityservice)
