pkgname=$(basename $(pwd))
pkgver=0.13
pkgrel=1
pkgdesc='diet4j Java module management'
arch=('any')
url="http://jdiet.org/"
license=('Apache')
makedepends=('maven' 'jdk8-openjdk' )
depends=('java-runtime=8')

prepare() {
    # Set pom.xml versions correctly; depends on XML-comment-based markup in pom.xml files
    # This is a great big hack, but does the job
    find ${startdir} -name pom.xml -exec perl -pi -e "s/(?<=\<\!-- PKGVER -->)(\d+(\.\d+)+)(?=\<\!-- \/PKGVER -->)/${pkgver}/g" {} \;
}

build() {
    cd ${startdir}
    mvn package install ${MVN_OPTS}
}

package() {
    # Jars
    installOne 'diet4j-cmdline'
    installOne 'diet4j-core'
    installOne 'diet4j-inclasspath'
    installOne 'diet4j-tomcat'
    installOne 'diet4j-status'

    # Command-line
    install -m755 -D ${startdir}/diet4j-cmdline/bin/diet4j ${pkgdir}/usr/bin/diet4j
    perl -pi -e "s/^VERSION=\d+(\.\d+)*/VERSION=${pkgver}/" ${pkgdir}/usr/bin/diet4j

    # Tomcat
    mkdir -p ${pkgdir}/usr/share/java/tomcat8
    ln -s /usr/lib/java/org/diet4j/diet4j-tomcat/${pkgver}/diet4j-tomcat-${pkgver}.jar ${pkgdir}/usr/share/java/tomcat8/diet4j-tomcat-${pkgver}.jar
}

installOne() {
    local name=$1 
    install -m644 -D ${startdir}/${name}/target/${name}-${pkgver}.jar ${pkgdir}/usr/lib/java/org/diet4j/${name}/${pkgver}/${name}-${pkgver}.jar
}

