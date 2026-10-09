#! /usr/bin/bash
# print a ';' separated list of absolute paths to DLLs used by the input exe and Qt plugins
# output intended for use by CMake file(INSTALL ...)
# by default, plugins includes: qsqlite qsqlpsql qsqlmysql
set -e

if [[ $# -lt 2 ]] ;then
    echo "usage: $0 <MXE-target-root> {<exe> | <dll>} [--no-sql-drivers] [<plugin-name> ...]" >&2
    exit 1
fi

if [[ ! -d "$1/bin" || ! -d "$1/qt6/bin" ]] ;then
    echo "invalid MXE target root (missing bin or qt6/bin): $1" >&2
    exit 1
fi

if [[ ! -f "$2" ]] ;then
    echo "file not found: $2" >&2
    exit 1
fi

mxe_target_root=$1
search=("$mxe_target_root/bin" "$mxe_target_root/qt6/bin")
processed=(KERNEL32.dll SHELL32.dll msvcrt.dll)

function get_imports {
    infile=$1
    dlls=($(objdump -p "$infile"|sed -En -e "/DLL Name:/{s,[\\t ]+DLL Name: *,,;p}"))

    for dll in ${dlls[@]} ;do
        if [[ "${processed[@]}" != *$dll* && "${unprocessed[@]}" != *$dll* ]] ;then
            unprocessed=($dll ${unprocessed[@]})
        fi
    done
}

get_imports $2
shift 2

if [[ "$1" != "--no-sql-drivers" ]] ;then
    plugins=(qsqlite qsqlpsql qsqlmysql)
else
    shift 1
fi
plugins+=($@)

for name in ${plugins[@]} ;do
    plugin=$(find "$mxe_target_root/qt6/plugins" -name "$name.dll")
    if [[ -f "$plugin" ]] ;then
        get_imports $plugin
    else
        echo "plugin not found: $name" >&2
    fi
done

while [[ ${#unprocessed[@]} -gt 0 ]] ;do
    file=${unprocessed[-1]}
    processed+=($file)
    for dir in ${search[@]} ;do
        # look for name as-is and in lowercase
        for check_file in "$dir/$file" "$dir/${file,,}" ;do
            if [[ -f "$check_file" ]] ;then
                echo -n $check_file\;
                get_imports "$check_file"
                unset file
                break;
            fi
        done
    done
    if [[ -n "$file" ]] ;then
        echo "not found: $file" >&2
    fi
    unset unprocessed[-1]
done
