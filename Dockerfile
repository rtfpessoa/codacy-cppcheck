FROM codacy-cppcheck-base:latest

LABEL maintainer="team@codacy.com"

RUN adduser -u 2004 -D docker
WORKDIR /opt/docker

COPY --chown=docker:docker "target/docker/stage/opt/docker" "/opt/docker"
COPY --chown=docker:docker src/main/resources/docs /docs

USER docker
ENTRYPOINT ["bin/codacy-cppcheck"]
CMD []
