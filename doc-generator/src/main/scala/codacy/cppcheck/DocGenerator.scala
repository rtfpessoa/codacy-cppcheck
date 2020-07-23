package codacy.cppcheck

import play.api.libs.json.{JsArray, Json}
import better.files._
import better.files

import scala.xml.{Elem, XML}

object DocGenerator {

  case class Ruleset(patternId: String, level: String, title: String, description: String)

  def main(args: Array[String]): Unit = {

    val version: String = Versions.cppcheckVersion
    val fileName = args(0)
    val rules = getRules(fileName)
    createPatternsAndDescriptionFile(version, rules)
  }

  private def generatePatterns(rules: Seq[Ruleset]): JsArray = {
    val codacyPatterns = rules.map { rule =>
      val category: String =
        if (rule.level.startsWith("error") ||
          rule.level.startsWith("warning") ||
          rule.level.startsWith("portability")) {
          "ErrorProne"
        } else if (rule.level.startsWith("performance")) {
          "Performance"
        } else {
          "CodeStyle"
        }

      val level: String =
        if (rule.level.startsWith("warning") ||
          rule.level.startsWith("portability") ||
          rule.level.startsWith("performance")) {
          "Warning"
        } else if (rule.level.startsWith("error")) {
          "Error"
        } else {
          "Info"
        }

      Json.obj("patternId" -> rule.patternId, "level" -> level, "category" -> category)

    }
    Json.parse(Json.toJson(codacyPatterns).toString).as[JsArray]
  }

  private def generateDescriptions(rules: Seq[Ruleset]): JsArray = {
    val codacyPatternsDescs = rules.map { rule =>
      Json.obj(
        "patternId" -> rule.patternId,
        "title" -> Json.toJsFieldJsValueWrapper(rule.title),
        "description" -> Json.toJsFieldJsValueWrapper(truncateText(rule.description, 495)),
        "timeToFix" -> 5
      )
    }

    Json.parse(Json.toJson(codacyPatternsDescs).toString).as[JsArray]
  }

  private def getRules(fileName: String): Seq[Ruleset] = {
    val outputXML: Elem = XML.loadFile(fileName)
    (outputXML \\ "errors" \\ "error").map { r =>
      Ruleset((r \ "@id").text, (r \ "@severity").text, (r \ "@msg").text, (r \ "@verbose").text)
    }
  }

  private def createPatternsAndDescriptionFile(version: String, rules: Seq[DocGenerator.Ruleset]): Unit = {
    val repoRoot: files.File = File(".")
    val docsRoot: files.File = File(repoRoot, "docs")
    val patternsFile: files.File = File(docsRoot, "patterns.json")
    val descriptionsRoot: files.File = File(docsRoot, "description")
    val descriptionsFile: files.File =
      File(descriptionsRoot, "description.json")

    val patterns: String = getPatterns(version, rules)
    val descriptions: String = getDescriptions(rules)

    patternsFile.write(s"${patterns}${System.lineSeparator}")
    descriptionsFile.write(s"${descriptions}${System.lineSeparator}")
  }

  private def misraPatternId(rule: String): String = s"misra-c2012-$rule"

  private def getMisraPatterns(): JsArray = {
    val misraPatterns = getMisraRules.map { rule =>
      Json.obj("patternId" -> misraPatternId(rule), "level" -> "Warning", "category" -> "ErrorProne")
    }

    Json.parse(Json.toJson(misraPatterns).toString).as[JsArray]
  }

  private def getMisraRules(): Seq[String] = {
    val misraRulesLines = File("addons/misra_rules.txt").lines
    misraRulesLines.filter(_.startsWith("Rule ")).map(_.stripPrefix("Rule ")).toSeq
  }

  private def getMisraDescription(): JsArray = {
    val misraDescriptions = getMisraRules.map { rule =>
      Json.obj(
        "patternId" -> misraPatternId(rule),
        "title" -> Json.toJsFieldJsValueWrapper(s"MISRA $rule rule"),
        "timeToFix" -> 5
      )
    }

    Json.parse(Json.toJson(misraDescriptions).toString).as[JsArray]
  }

  private def getAddonPatterns(): JsArray = {
    val patternsJson = File("addons/patterns.json").contentAsString
    (Json.parse(patternsJson) \ "patterns").as[JsArray] ++ getMisraPatterns()
  }

  private def getAddonDescription(): JsArray = {
    val descriptionJson = File("addons/description/description.json").contentAsString
    Json.parse(descriptionJson).as[JsArray] ++ getMisraDescription()
  }

  private def getPatterns(version: String, rules: Seq[DocGenerator.Ruleset]): String = {
    Json.prettyPrint(
      Json.obj(
        "name" -> "cppcheck",
        "version" -> version,
        "patterns" -> (Json
          .parse(Json.toJson(generatePatterns(rules)).toString)
          .as[JsArray] ++ getAddonPatterns())
      )
    )
  }

  private def getDescriptions(rules: Seq[DocGenerator.Ruleset]): String = {
    Json.prettyPrint(
      Json
        .parse(Json.toJson(generateDescriptions(rules)).toString)
        .as[JsArray] ++ getAddonDescription()
    )
  }

  private def truncateText(description: String, maxCharacters: Int): String = {
    if (description.length > maxCharacters) {
      description
        .take(maxCharacters)
        .split("\\.")
        .dropRight(1)
        .mkString(".") + "."
    } else {
      description
    }
  }
}
