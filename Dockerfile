FROM codacy/codacy-cppcheck-base:latest

RUN adduser -u 2004 -D docker
WORKDIR /opt/docker

ADD --chown=docker:docker "target/docker/stage/opt/docker" "/opt/docker"
ADD --chown=docker:docker src/main/resources/docs /docs

USER docker
ENTRYPOINT ["bin/codacy-cppcheck"]
CMD []