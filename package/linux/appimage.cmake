# fix Exec in desktop file
find_file(SED sed ${PATH})
execute_process(
    COMMAND ${SED} -i -e "/^Exec=/s|/usr/bin/||" "Finances-qt.desktop"
    WORKING_DIRECTORY "${CPACK_TEMPORARY_INSTALL_DIRECTORY}/share/applications"
    COMMAND_ECHO STDOUT
    COMMAND_ERROR_IS_FATAL ANY
)

# add link to icon
find_file(SHELL sh ${PATH})
execute_process(
    COMMAND ${SHELL} -c "ln -s share/icons/finances-qt.png ."
    WORKING_DIRECTORY "${CPACK_TEMPORARY_INSTALL_DIRECTORY}"
    COMMAND_ECHO STDOUT
    COMMAND_ERROR_IS_FATAL ANY
)
