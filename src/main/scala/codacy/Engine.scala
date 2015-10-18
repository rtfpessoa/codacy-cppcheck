package codacy

import codacy.cppcheck.CPPCheck
import codacy.dockerApi.DockerEngine

object Engine extends DockerEngine(CPPCheck)
