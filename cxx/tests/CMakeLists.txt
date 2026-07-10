qt_add_executable(test_decimal test_decimal.cpp)
set_target_properties(test_decimal PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
)

add_dependencies(test_decimal qdecimal)

set_target_properties(test_decimal PROPERTIES
    AUTOMOC ON
    WIN32_EXECUTABLE TRUE
    MACOSX_BUNDLE TRUE
)

target_include_directories(test_decimal PRIVATE
    ${QDECIMAL_SOURCE_DIR}/src
    ${QDECIMAL_SOURCE_DIR}/decnumber
)

target_link_libraries(test_decimal PRIVATE
    Qt6::Core
    Qt6::Gui
    Qt6::Test
    Qt6::Widgets
    ${QDECIMAL_BINARY_DIR}/src/libqdecimal.a
    ${QDECIMAL_BINARY_DIR}/decnumber/libdecnumber.a
)

install(TARGETS test_decimal
    BUNDLE  DESTINATION .
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

# qt_generate_deploy_app_script(
#     TARGET test_decimal
#     OUTPUT_SCRIPT deploy_test_decimal
#     NO_UNSUPPORTED_PLATFORM_ERROR
# )
# install(SCRIPT ${deploy_test_decimal})

add_test(NAME test_decimal COMMAND test_decimal)

get_target_property(UI_SOURCES finances SOURCES)
list(FILTER UI_SOURCES INCLUDE REGEX ".*/ui/.*\\.(cpp|h)$")

qt_add_library(finances-testlib OBJECT
    ${UI_SOURCES}
)

set_target_properties(finances-testlib PROPERTIES
    COMPILE_DEFINITIONS __FINANCES_TEST__
)

target_link_libraries(finances-testlib PUBLIC
    Qt${QT_VERSION_MAJOR}::Core
    Qt${QT_VERSION_MAJOR}::Gui
    Qt${QT_VERSION_MAJOR}::Widgets
    Qt${QT_VERSION_MAJOR}::Sql
    ${QDECIMAL_BINARY_DIR}/src/libqdecimal.a
    ${QDECIMAL_BINARY_DIR}/decnumber/libdecnumber.a
    ${QT_SQLITE_OBJ_FILE}
)

target_include_directories(finances-testlib PRIVATE
    $<TARGET_PROPERTY:finances,INCLUDE_DIRECTORIES>
)
