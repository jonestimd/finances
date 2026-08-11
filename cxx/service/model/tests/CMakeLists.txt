function(add_domain_model_test name)
    set(exec_name "test_${name}")
    qt_add_executable(${exec_name} ${exec_name}.cpp ${ARGN})
    add_dependencies(${exec_name} qdecimal decimal.so)
    set_target_properties(${exec_name} PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
    )
    target_include_directories(${exec_name} PRIVATE
        $<TARGET_PROPERTY:finances,INCLUDE_DIRECTORIES>
    )
    target_link_libraries(${exec_name} PRIVATE
        Qt${QT_VERSION_MAJOR}::Test
        finances-service
    )
    add_test(NAME ${exec_name}
        WORKING_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
        COMMAND ${exec_name}
    )
endFunction()

add_domain_model_test(transaction)
