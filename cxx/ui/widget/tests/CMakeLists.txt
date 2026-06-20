function(add_widget_test name)
    set(exec_name "test_${name}")
    qt_add_executable(${exec_name} ${exec_name}.cpp ../${name}.cpp ../${name}.h ${PROJECT_RESOURCES} ${ARGN})
    # add_dependencies(${exec_name} qdecimal decimal.so dbtestcase finances-testlib)
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
        Qt${QT_VERSION_MAJOR}::Widgets
        Qt${QT_VERSION_MAJOR}::Test
    )

    add_test(NAME ${exec_name} COMMAND ${exec_name})
endfunction()

function(add_window_test name)
    set(exec_name "test_${name}")
    qt_add_executable(${exec_name} ${exec_name}.cpp ${PROJECT_RESOURCES} ${ARGN})
    add_dependencies(${exec_name} qdecimal decimal.so dbtestcase finances-testlib)
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
        finances-testlib
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

    add_test(NAME ${exec_name} COMMAND LD_LIBRARY_PATH="${SQLITE_EXTENSION_DIR}" ${exec_name})
endfunction()

add_widget_test(treeview)
add_window_test(transactionswindow)
