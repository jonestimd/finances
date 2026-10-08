function(add_widget_test name)
    set(exec_name "test_${name}")
    qt_add_executable(${exec_name} ${exec_name}.cpp ${PROJECT_RESOURCES} ${ARGN})
    set_target_properties(${exec_name} PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
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
        finances-ui
        finances-service
    )

    add_test(NAME ${exec_name} COMMAND ${exec_name} -platform offscreen)
endfunction()

function(add_window_test name)
    set(exec_name "test_${name}")
    qt_add_executable(${exec_name} ${exec_name}.cpp ${PROJECT_RESOURCES} ${ARGN})
    add_dependencies(${exec_name} qdecimal)
    set_target_properties(${exec_name} PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
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
        finances-ui
        finances-uitest
        finances-service
    )

    add_test(NAME ${exec_name}
        WORKING_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
        COMMAND ${exec_name} -platform offscreen
    )
endfunction()

add_widget_test(treeview)
add_window_test(transactionswindow)
add_window_test(connectiondialog)
