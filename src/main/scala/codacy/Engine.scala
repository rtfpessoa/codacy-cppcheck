package codacy

import codacy.cppcheck.CPPCheck
import com.codacy.tools.scala.seed.DockerEngine

object Engine extends DockerEngine(CPPCheck)()
