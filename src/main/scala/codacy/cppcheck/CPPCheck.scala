package codacy.cppcheck

import java.nio.file.Path

import codacy.dockerApi._
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

  override def apply(path: Path, conf: Option[List[PatternDef]], files: Option[Set[Path]])(implicit spec: Spec): Try[List[Result]] = {
    Try {

      val filesToLint: Seq[String] = files.fold(Seq(path.toString)) { paths =>
        paths.map(_.toString).toSeq
      }

      val command = List("cppcheck", "--enable=all", "--error-exitcode=0", "--force",
        """--template={"patternId":"{id}","file":"{file}","line":"{line}","message":"{message}"}""") ++
        filesToLint

      CommandRunner.exec(command) match {
        case Right(resultFromTool) =>
          val output = resultFromTool.stdout ++ resultFromTool.stderr
          parseToolResult(output, path, checkPattern(conf)).filterNot(blacklisted)
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
    } yield Issue(SourcePath(result.file), ResultMessage(result.message),
      PatternId(result.patternId), ResultLine(line))
  }

  private def checkPattern(conf: Option[List[PatternDef]])(patternId: String): Boolean = {
    conf.forall(_.exists(_.patternId.value.toLowerCase == patternId.toLowerCase))
  }

  def blacklisted(result: Result): Boolean = {
    result match {
      case issue: Issue => blacklist.contains(issue.patternId.value)
      case _ => false
    }
  }

}
