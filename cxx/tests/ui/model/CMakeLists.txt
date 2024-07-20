qt_add_executable(test_comboboxmodel
    test_comboboxmodel.cpp
    ${CMAKE_HOME_DIRECTORY}/cxx/ui/model/comboboxmodel.cpp
    ${CMAKE_HOME_DIRECTORY}/cxx/service/model/basedomain.cpp
)

set_target_properties(test_comboboxmodel PROPERTIES
    AUTOMOC ON
    WIN32_EXECUTABLE TRUE
    MACOSX_BUNDLE TRUE
)

target_include_directories(test_comboboxmodel PRIVATE
    ${CMAKE_HOME_DIRECTORY}/cxx
)
target_link_libraries(test_comboboxmodel PRIVATE
    Qt6::Core
    Qt6::Gui
    Qt6::Sql
    Qt6::Test
    Qt6::Widgets
)

install(TARGETS test_comboboxmodel
    BUNDLE  DESTINATION .
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

qt_generate_deploy_app_script(
    TARGET test_comboboxmodel
    OUTPUT_SCRIPT deploy_test_comboboxmodel
    NO_UNSUPPORTED_PLATFORM_ERROR
)
install(SCRIPT ${deploy_test_comboboxmodel})
