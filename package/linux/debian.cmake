# move Qt libs and plugins to /opt/finances
find_file(SHELL sh ${PATH})
execute_process(
    COMMAND ${SHELL} -c "mkdir -p opt/finances && mv usr/lib opt/finances && mv usr/plugins opt/finances"
    WORKING_DIRECTORY "$ENV{DESTDIR}"
    COMMAND_ECHO STDOUT
    COMMAND_ERROR_IS_FATAL ANY
)
