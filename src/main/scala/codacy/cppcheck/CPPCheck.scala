package codacy.cppcheck

import java.nio.file.{Path, Paths}

import codacy.docker.api.Result.Issue
import codacy.docker.api.{Configuration, Pattern, Result, Source, Tool}
import codacy.dockerApi.utils.CommandRunner
import play.api.libs.json._

import scala.util.Try

case class WarnResult(patternId: String, file: String, message: String, line: String = "1")

object WarnResult {
  implicit val warnResultFmt = Json.format[WarnResult]
}

object CPPCheck extends Tool {

  lazy val blacklist = Set(
    "unusedFunction"
  )

  private val languageConfKey = Configuration.Key("language")

  override def apply(source: Source.Directory, configuration: Option[List[Pattern.Definition]], files: Option[Set[Source.File]],
                     options: Map[Configuration.Key, Configuration.Value])
                    (implicit specification: Tool.Specification): Try[List[Result]] = {
    Try {
      val path = Paths.get(source.path)
      val filesToLint: Seq[String] = files.fold(Seq(source.path)) { paths =>
        paths.map(_.toString).toSeq
      }

      val languageParameter: String = options.get(languageConfKey)
        .flatMap { language =>
          Option(language: JsValue).collect {
            case JsString(lang) => s"--language=$lang"
          }
        }.getOrElse("")

      val command = List("cppcheck", "--enable=all", "--error-exitcode=0", "--force", "-j 2", languageParameter,
        """--template={"patternId":"{id}","file":"{file}","line":"{line}","message":"{message}"}""") ++
        filesToLint

      CommandRunner.exec(command) match {
        case Right(resultFromTool) =>
          val output = resultFromTool.stdout ++ resultFromTool.stderr
          parseToolResult(output, path, checkPattern(configuration)).filterNot(blacklisted)
        case Left(failure) =>
          throw failure
      }
    }
  }

  private def parseToolResult(resultFromTool: List[String], path: Path, wasRequested: String => Boolean): List[Result] = {
    for {
      outputLine <- resultFromTool
      result <- Try(Json.parse(outputLine)).toOption.flatMap(_.asOpt[WarnResult]) if wasRequested(result.patternId)
      line <- Try(result.line.toInt).toOption
    } yield Issue(Source.File(result.file), Result.Message(result.message),
      Pattern.Id(result.patternId), Source.Line(line))
  }

  private def checkPattern(conf: Option[List[Pattern.Definition]])(patternId: String): Boolean = {
    conf.forall(_.exists(_.patternId.value.toLowerCase == patternId.toLowerCase))
  }

  def blacklisted(result: Result): Boolean = {
    result match {
      case issue: Issue => blacklist.contains(issue.patternId.value)
      case _ => false
    }
  }

}
