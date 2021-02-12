FROM codacy-cppcheck-base as dev

RUN apk add openjdk11
COPY docs /docs
COPY addons/misra* /workdir/addons/
RUN adduser --uid 2004 --disabled-password --gecos "" docker
COPY target/universal/stage/ /workdir/
RUN chmod +x /workdir/bin/codacy-cppcheck
USER docker
WORKDIR /workdir
ENTRYPOINT ["bin/codacy-cppcheck"]

FROM codacy-cppcheck-base

COPY docs /docs
COPY addons/misra* /workdir/addons/
RUN adduser --uid 2004 --disabled-password --gecos "" docker
COPY target/graalvm-native-image/codacy-cppcheck /workdir/
USER docker
WORKDIR /workdir
ENTRYPOINT ["/workdir/codacy-cppcheck"]
