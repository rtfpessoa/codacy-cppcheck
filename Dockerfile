FROM alpine:3.12 as base

ARG toolVersion=2.1

RUN \
    # runtime packages
    apk add --no-cache bash python3 pcre z3 && \
    # compile packages
    apk add --no-cache -t .required_apks wget make g++ pcre-dev z3-dev && \
    wget --no-check-certificate -O /tmp/cppcheck.tar.gz https://github.com/danmar/cppcheck/archive/"$toolVersion".tar.gz && \
    tar -zxf /tmp/cppcheck.tar.gz -C /tmp && \
    cd /tmp/cppcheck-$toolVersion && \
    make install -j$(nproc) CFGDIR=/cfg MATCHCOMPILER=yes FILESDIR=/usr/share/cppcheck HAVE_RULES=yes USE_Z3=yes \
        CXXFLAGS="-O2 -DNDEBUG -Wall -Wno-sign-compare -Wno-unused-function" && \
    apk del .required_apks && \
    rm -rf /tmp/* && \
    rm -rf /var/cache/apk/*

COPY docs /docs
COPY addons /addons
RUN adduser --uid 2004 --disabled-password --gecos "" docker

FROM base as dev

RUN apk add openjdk11
COPY target/universal/stage/ /workdir/
RUN chmod +x /workdir/bin/codacy-cppcheck
USER docker
WORKDIR /src
ENTRYPOINT ["/workdir/bin/codacy-cppcheck"]

FROM base

COPY target/graalvm-native-image/codacy-cppcheck /workdir/
USER docker
WORKDIR /src
ENTRYPOINT ["/workdir/codacy-cppcheck"]
