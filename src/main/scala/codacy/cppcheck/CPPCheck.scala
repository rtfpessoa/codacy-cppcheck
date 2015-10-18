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

  override def apply(path: Path, conf: Option[Seq[PatternDef]], files: Option[Set[Path]])(implicit spec: Spec): Try[Iterable[Result]] = {
    Try {
      val filesToLint: Seq[String] = files.fold(Seq(path.toString)) { paths =>
        paths.map(_.toString).toSeq
      }

      val command = Seq("cppcheck", "--enable=all", "--error-exitcode=0",
        """--template={"patternId":"{id}","file":"{file}","line":"{line}","message":"{message}"}""") ++
        filesToLint

      CommandRunner.exec(command) match {
        case Right(resultFromTool) =>
          val output = resultFromTool.stdout ++ resultFromTool.stderr
          parseToolResult(output, path, checkPattern(conf))
        case Left(failure) => throw failure
      }
    }
  }

  private def parseToolResult(resultFromTool: Seq[String], path: Path, wasRequested: String => Boolean): Seq[Result] = {
    for {
      outputLine <- resultFromTool
      result <- Try(Json.parse(outputLine)).toOption.flatMap(_.asOpt[WarnResult]) if wasRequested(result.patternId)
      line <- Try(result.line.toInt).toOption
    } yield Issue(SourcePath(result.file), ResultMessage(result.message),
      PatternId(result.patternId), ResultLine(line))
  }

  private def checkPattern(conf: Option[Seq[PatternDef]])(patternId: String): Boolean = {
    conf
      .map(_.exists(_.patternId.value.toLowerCase == patternId.toLowerCase))
      .getOrElse(true)
  }

}