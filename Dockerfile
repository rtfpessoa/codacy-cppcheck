FROM ubuntu:17.10
ARG toolVersion

RUN apt-get update
RUN apt-get install -y --no-install-recommends bash wget make g++ libpcre3-dev
RUN wget --no-check-certificate -O /tmp/cppcheck.tar.gz https://github.com/danmar/cppcheck/archive/$toolVersion.tar.gz
RUN tar -zxf /tmp/cppcheck.tar.gz -C /tmp
WORKDIR /tmp/cppcheck-$toolVersion
RUN make install CFGDIR=/cfg HAVE_RULES=yes CXXFLAGS="-O2 -DNDEBUG -Wall -Wno-sign-compare -Wno-unused-function --static -pthread"
RUN rm -rf /tmp/*
RUN rm -rf /var/cache/apk/*

CMD ["cppcheck", "--errorlist"]
