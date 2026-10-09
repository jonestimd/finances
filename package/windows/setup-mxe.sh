#! /usr/bin/bash
set -e
postgresql_version=18.6
mxe_dir=${1:-${MXE_DIR:-/opt/mxe}}

if [[ "${mxe_dir:0:1}" != "/" ]] ;then
    echo "invalid MXE_DIR (should be an absolute path): ${mxe_dir}"
    exit 1
fi

echo "using $mxe_dir for cross-compile"

if [[ ! -d "${mxe_dir}" ]] ;then
    cd $(dirname "${mxe_dir}")
    git clone https://github.com/mxe/mxe.git $(basename "${mxe_dir}")
fi
cd "${mxe_dir}"
git restore .
git pull

cat <<END > settings.mk
qt6-qtbase_CMAKE_OPTS := -DFEATURE_sql_psql=ON
qt6-qtbase_x86_64-pc-linux-gnu_CMAKE_OPTS := -DFEATURE_sql_psql=OFF
MXE_TARGETS := x86_64-w64-mingw32.shared
JOBS := 8
END

# update PostgreSQL version
sed -i -e "/^[$](PKG)_VERSION/s,:=.*$,:= ${postgresql_version}," \
    -e "/grep/s,REL_[^[]*,REL_18," \
    -e "/ +&& autoconf/s, +&& autoconf,," \
    -e "/--without-krb5/s,krb5,icu," \
    -e "/pg_config_ext\\.h/d" \
    -e "/ && autoconf$/s, && autoconf,," \
    src/postgresql.mk
rm src/postgresql-*.patch
make update-checksum-postgresql
# make update-package-postgresql
make update-package-sqlite update-package-nsis
make postgresql sqlite nsis

make qt6-qtbase qt6-qt5compat
