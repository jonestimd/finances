qt_add_executable(testdecimal testdecimal.cpp)

add_dependencies(testdecimal qdecimal)

set_target_properties(testdecimal PROPERTIES
    AUTOMOC ON
    WIN32_EXECUTABLE TRUE
    MACOSX_BUNDLE TRUE
)

target_include_directories(testdecimal PRIVATE
    ${SOURCE_DIR}/src
    ${SOURCE_DIR}/decnumber
)

target_link_libraries(testdecimal PRIVATE
    Qt6::Core
    Qt6::Gui
    Qt6::Test
    Qt6::Widgets
    ${BINARY_DIR}/src/libqdecimal.a
    ${BINARY_DIR}/decnumber/libdecnumber.a
)

install(TARGETS testdecimal
    BUNDLE  DESTINATION .
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

qt_generate_deploy_app_script(
    TARGET testdecimal
    OUTPUT_SCRIPT deploy_testdecimal
    NO_UNSUPPORTED_PLATFORM_ERROR
)
install(SCRIPT ${deploy_testdecimal})

add_test(NAME testdecimal COMMAND testdecimal)