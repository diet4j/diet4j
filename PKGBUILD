pkgname=$(basename $(pwd))
pkgver=0.25
pkgrel=1
pkgdesc='diet4j Java module management'
arch=('any')
url="http://diet4j.org/"
license=('Apache')
makedepends=('maven' 'jdk11-openjdk' )
depends=('java-runtime')
optdepends=('java-jsvc')
_groupId='org.diet4j'
options=('!strip')
_m2repo=${GRADLE_M2_HOME:-${HOME}/.m2/repository}

prepare() {
    # Set pom.xml versions correctly; depends on XML-comment-based markup in pom.xml files
    # This is a great big hack, but does the job
    find ${startdir} -path ${startdir}/pkg -prune -o -name pom.xml -exec perl -pi -e "s/(?<=\<\!-- PKGVER -->)(\d+(\.\d+)+)(?=\<\!-- \/PKGVER -->)/${pkgver}/g" {} \;

    # Also do this for config files
    perl -pi -e "s/^DIET4J_VERSION=.*$/DIET4J_VERSION=${pkgver}/" ${startdir}/diet4j-cmdline/etc/diet4j-defaults.conf
    perl -pi -e "s/^DIET4J_VERSION=.*$/DIET4J_VERSION=${pkgver}/" ${startdir}/diet4j-jsvc/etc/diet4j-jsvc-defaults.conf
}

build() {
    cd ${startdir}

    if [[ "${GRADLE_M2_HOME}" != '' ]]; then
        export M2_HOME=${_m2repo}
    fi

    mvn package install ${MVN_OPTS}
}

package() {
    # Jars
    installOne 'diet4j-cmdline'
    installOne 'diet4j-core'
    installOne 'diet4j-inclasspath'
    installOne 'diet4j-jsvc'
    installOne 'diet4j-status'
    install -m644 -D ${_m2repo}/${_groupId//.//}/diet4j/${pkgver}/diet4j-${pkgver}.pom -t ${pkgdir}/usr/lib/java/org/diet4j/diet4j/${pkgver}/

    # Command-line
    install -m755 -D ${startdir}/diet4j-cmdline/bin/diet4j ${pkgdir}/usr/bin/diet4j
    perl -pi -e "s/^VERSION=.*$/VERSION=\\\${DIET4J_VERSION:-${pkgver}}/" ${pkgdir}/usr/bin/diet4j

    # JSVC
    mkdir -p ${pkgdir}/usr/lib/java/org/diet4j/diet4j-jsvc/current
    ln -s ../${pkgver}/diet4j-jsvc-${pkgver}.jar ${pkgdir}/usr/lib/java/org/diet4j/diet4j-jsvc/current/diet4j-jsvc-current.jar

    # Settings
    install -m644 -D ${startdir}/diet4j-cmdline/etc/*.{conf,properties} -t ${pkgdir}/etc/diet4j/
    install -m644 -D ${startdir}/diet4j-jsvc/etc/*.conf -t ${pkgdir}/etc/diet4j/

    # Systemd
    mkdir -p ${pkgdir}/usr/lib/systemd/system
    install -m 644 ${startdir}/diet4j-jsvc/systemd/diet4j-jsvc@.service -t ${pkgdir}/usr/lib/systemd/system
}

installOne() {
    local name=$1
    install -m644 -D ${_m2repo}/${_groupId//.//}/${name}/${pkgver}/${name}-${pkgver}.{jar,pom} -t ${pkgdir}/usr/lib/java/org/diet4j/${name}/${pkgver}/
}
