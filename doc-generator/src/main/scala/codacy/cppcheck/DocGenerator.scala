package codacy.cppcheck

import play.api.libs.json.{JsArray, Json}
import better.files._
import better.files

import scala.xml.{Elem, XML}
import scala.sys.process._

object DocGenerator {

  case class Ruleset(patternId: String, level: String, title: String, description: String)

  def main(args: Array[String]): Unit = {
    val rules = (for {
      file <- File.temporaryFile()
      javaFile = file.toJava
      _ = assert("docker run -i --entrypoint cppcheck codacy-cppcheck-base:latest --errorlist".#>(javaFile).! == 0)
      res = getRules(javaFile)
    } yield res).get()
    createPatternsAndDescriptionFile(rules)
  }

  private val misraTitles = Map(
    "1.1" -> "The program shall contain no violations of the standard C syntax and constraints, and shall not exceed the implementation's translation limits",
    "1.2" -> "Language extensions should not be used",
    "1.3" -> "There shall be no occurrence of undefined or critical unspecified behaviour",
    "1.4" -> "Emergent language features shall not be used",
    "2.1" -> "A project shall not contain unreachable code",
    "2.2" -> "There shall be no dead code",
    "2.3" -> "A project should not contain unused type declarations",
    "2.4" -> "A project should not contain unused tag declarations",
    "2.5" -> "A project should not contain unused macro declarations",
    "2.6" -> "A function should not contain unused label declarations",
    "2.7" -> "There should be no unused parameters in functions",
    "3.1" -> "The character sequences /* and // shall not be used within a comment",
    "3.2" -> "Line-splicing shall not be used in // comments",
    "4.1" -> "Octal and hexadecimal escape sequences shall be terminated",
    "4.2" -> "Trigraphs should not be used",
    "5.1" -> "External identifiers shall be distinct",
    "5.2" -> "Identifiers declared in the same scope and name space shall be distinct",
    "5.3" -> "An identifier declared in an inner scope shall not hide an identifier declared in an outer scope",
    "5.4" -> "Macro identifiers shall be distinct",
    "5.5" -> "Identifiers shall be distinct from macro names",
    "5.6" -> "A typedef name shall be a unique identifier",
    "5.7" -> "A tag name shall be a unique identifier",
    "5.8" -> "Identifiers that define objects or functions with external linkage shall be unique",
    "5.9" -> "Identifiers that define objects or functions with internal linkage should be unique",
    "6.1" -> "Bit-fields shall only be declared with an appropriate type",
    "6.2" -> "Single-bit named bit fields shall not be of a signed type",
    "7.1" -> "Octal constants shall not be used",
    "7.2" -> "A 'u' or 'U' suffix shall be applied to all integer constants that are represented in an unsigned type",
    "7.3" -> "The lowercase character 'l' shall not be used in a literal suffix",
    "7.4" -> "A string literal shall not be assigned to an object unless the object's type is 'pointer to const- qualified char'",
    "8.1" -> "Types shall be explicitly specified",
    "8.2" -> "Function types shall be in prototype form with named parameters",
    "8.3" -> "All declarations of an object or function shall use the same names and type qualifiers",
    "8.4" -> "A compatible declaration shall be visible when an object or function with external linkage is defined",
    "8.5" -> "An external object or function shall be declared once in one and only one file",
    "8.6" -> "An identifier with external linkage shall have exactly one external definition",
    "8.7" -> "Functions and objects should not be defined with external linkage if they are referenced in only one translation unit",
    "8.8" -> "The static storage class specifier shall be used in all declarations of objects and functions that have internal linkage",
    "8.9" -> "An object should be defined at block scope if its identifier only appears in a single function",
    "8.10" -> "An inline function shall be declared with the static storage class",
    "8.11" -> "When an array with external linkage is declared, its size should be explicitly specified",
    "8.12" -> "Within an enumerator list, the value of an implicitly-specified enumeration constant shall be unique",
    "8.13" -> "A pointer should point to a const-qualified type whenever possible",
    "8.14" -> "The restrict type qualifier shall not be used",
    "9.1" -> "The value of an object with automatic storage duration shall not be read before it has been set",
    "9.2" -> "The initializer for an aggregate or union shall be enclosed in braces",
    "9.3" -> "Arrays shall not be partially initialized",
    "9.4" -> "An element of an object shall not be in initialized more than once",
    "9.5" -> "Where designated initializers are used to initialize an array object the size of the array shall be specified explicitly",
    "10.1" -> "Operands shall not be of an inappropriate essential type",
    "10.2" -> "Expressions of essentially character type shall not be used inappropriately in addition and subtraction operations",
    "10.3" -> "The value of an expression shall not be assigned to an object with a narrower essential type or of a different essential type category",
    "10.4" -> "Both operands of an operator in which the usual arithmetic conversions are performed shall have the same essential type category",
    "10.5" -> "The value of an expression should not be cast to an inappropriate essential type",
    "10.6" -> "The value of a composite expression shall not be assigned to an object with wider essential type",
    "10.7" -> "If a composite expression is used as one operand of an operator in which the usual arithmetic conversions are performed then the other operand shall not have wider essential type",
    "10.8" -> "The value of a composite expression shall not be cast to a different essential type category or a wider essential type",
    "11.1" -> "Conversions shall not be performed between a pointer to a function and any other type",
    "11.2" -> "Conversions shall not be performed between a pointer to an incomplete type and any other type",
    "11.3" -> "A cast shall not be performed between a pointer to object type and a pointer to a different object type",
    "11.4" -> "A conversion should not be performed between a pointer to object and an integer type",
    "11.5" -> "A conversion should not be performed from pointer to void into pointer to object",
    "11.6" -> "A cast shall not be performed between pointer to void and an arithmetic type",
    "11.7" -> "A cast shall not be performed between pointer to object and a non-integer arithmetic type",
    "11.8" -> "A cast shall not remove any const or volatile qualification from the type pointed to by a pointer",
    "11.9" -> "The macro NULL shall be the only permitted form of integer null pointer constant",
    "12.1" -> "The precedence of operators within expressions should be made explicit",
    "12.2" -> "The right hand operand of a shift operator shall lie in the range zero to one less than the width in bits of the essential type of the left hand operand",
    "12.3" -> "The comma operator should not be used",
    "12.4" -> "Evaluation of constant expressions should not lead to unsigned integer wrap-around",
    "12.5" -> "The sizeof operator shall not have an operand which is a function parameter declared as 'array of type'",
    "13.1" -> "Initializer lists shall not contain persistent side effects",
    "13.2" -> "The value of an expression and its persistent side effects shall be the same under all permitted evaluation orders",
    "13.3" -> "A full expression containing an increment (++) or decrement (--) operator should have no other potential side effects other than that caused by the increment or decrement operator",
    "13.4" -> "The result of an assignment operator should not be used",
    "13.5" -> "The right hand operand of a logical && or '' operator shall not contain persistent side effects",
    "13.6" -> "The operand of the sizeof operator shall not contain any expression which has potential side effects",
    "14.1" -> "A loop counter shall not have essentially floating type",
    "14.2" -> "A for loop shall be well-formed",
    "14.3" -> "Controlling expressions shall not be invariant",
    "14.4" -> "The controlling expression of an if statement and the controlling expression of an iteration-statement shall have essentially Boolean type",
    "15.1" -> "The goto statement should not be used",
    "15.2" -> "The goto statement shall jump to a label declared later in the same function",
    "15.3" -> "Any label referenced by a goto statement shall be declared in the same block, or in any block enclosing the goto statement",
    "15.4" -> "There should be no more than one break or goto statement used to terminate any iteration statement",
    "15.5" -> "A function should have a single point of exit at the end",
    "15.6" -> "The body of an iteration-statement or a selection-statement shall be a compound-statement",
    "15.7" -> "All if ... else if constructs shall be terminated with an else statement",
    "16.1" -> "All switch statements shall be well-formed",
    "16.2" -> "A switch label shall only be used when the most closely-enclosing compound statement is the body of a switch statement",
    "16.3" -> "An unconditional break statement shall terminate every switch-clause",
    "16.4" -> "Every switch statement shall have a default label",
    "16.5" -> "A default label shall appear as either the first or the last switch label of a switch statement",
    "16.6" -> "Every switch statement shall have at least two switch-clauses",
    "16.7" -> "A switch-expression shall not have essentially Boolean type",
    "17.1" -> "The features of <stdarg.h> shall not be used",
    "17.2" -> "Functions shall not call themselves, either directly or indirectly",
    "17.3" -> "A function shall not be declared implicitly",
    "17.4" -> "All exit paths from a function with non-void return type shall have an explicit return statement with an expression",
    "17.5" -> "The function argument corresponding to a parameter declared to have an array type shall have an appropriate number of elements",
    "17.6" -> "The declaration of an array parameter shall not contain the static keyword between the [ ]",
    "17.7" -> "The value returned by a function having non-void return type shall be used",
    "17.8" -> "A function parameter should not be modified",
    "18.1" -> "A pointer resulting from arithmetic on a pointer operand shall address an element of the same array as that pointer operand",
    "18.2" -> "Subtraction between pointers shall only be applied to pointers that address elements of the same array",
    "18.3" -> "The relational operators >, >=, < and <= shall not be applied to objects of pointer type except where they point into the same object",
    "18.4" -> "The +, -, += and -= operators should not be applied to an expression of pointer type",
    "18.5" -> "Declarations should contain no more than two levels of pointer nesting",
    "18.6" -> "The address of an object with automatic storage shall not be copied to another object that persists after the first object has ceased to exist",
    "18.7" -> "Flexible array members shall not be declared",
    "18.8" -> "Variable-length array types shall not be used",
    "19.1" -> "An object shall not be assigned or copied to an overlapping object",
    "19.2" -> "The union keyword should not be used",
    "20.1" -> "#include directives should only be preceded by preprocessor directives or comments",
    "20.2" -> "The ', \" or \\ characters and the /* or // character sequences shall not occur in a header file name",
    "20.3" -> "The #include directive shall be followed by either a <filename> or \"filename\" sequence",
    "20.4" -> "A macro shall not be defined with the same name as a keyword",
    "20.5" -> "#undef should not be used",
    "20.6" -> "Tokens that look like a preprocessing directive shall not occur within a macro argument",
    "20.7" -> "Expressions resulting from the expansion of macro parameters shall be enclosed in parentheses",
    "20.10" -> "The # and ## preprocessor operators should not be used ",
    "20.13" -> "A line whose first token is # shall be a valid preprocessing directive",
    "20.14" -> "All #else, #elif and #endif preprocessor directives shall reside in the same file as the #if, #ifdef or #ifndef",    
    "21.3" -> "The memory allocation and deallocation functions of <stdlib.h> shall not be used",
    "21.4" -> "The standard header file <setjmp.h> shall not be used",
    "21.5" -> "The standard header file <signal.h> shall not be used",
    "21.6" -> "The Standard Library input/output routines shall not be used",
    "21.7" -> "The Standard Library functions atof, atoi, atol and atoll of <stdlib.h> shall not be used",
    "21.8" -> "The Standard Library termination functions of <stdlib.h> shall not be used",
    "21.9" -> "The library functions bsearch and qsort of <stdlib.h> shall not be used",
    "21.10" -> "The Standard Library time and date functions shall not be used",
    "21.11" -> "The standard header file <tgmath.h> shall not be used",
    "21.12" -> "The exception handling features of <fenv.h> should not be used",
    "21.13" -> "Any value passed to a function in <ctype.h> shall be representable as an unsigned char or be the value EOF",
    "21.14" -> "The Standard Library function memcmp shall not be used to compare null terminated strings",
    "21.15" -> "The pointer arguments to the Standard Library functions memcpy, memmove and memcmp shall be pointers to qualified or unqualified versions of compatible types",
    "21.16" -> "The pointer arguments to the Standard Library function memcmp shallpoint to either a pointer type, an essentially signed type, an essentially unsigned type, an essentially Boolean type or an essentially enum type",
    "21.17" -> "Use of the string handling functions from <string.h> shall not resultin accesses beyond the bounds of the objects referenced by their pointer parameters",
    "21.18" -> "The size_t argument passed to any function in <string.h> shall have an appropriate value",
    "21.19" -> "The pointers returned by the Standard Library functions localeconv, getenv, setlocale or, strerror shall only be used as if they have pointer to const-qualified type",
    "21.20" -> "The pointer returned by the Standard Library functions asctime, ctime, gmtime, localtime, localeconv, getenv, setlocale or strerror shall not be used following a subsequent call to the same function",
    "21.21" -> "The standard library function system of <stdlib.h> shall not be used",
    "22.1" -> "All resources obtained dynamically by means of Standard Library functions shall be explicitly released",
    "22.2" -> "A block of memory shall only be freed if it was allocated by means of a Standard Library function",
    "22.3" -> "The same file shall not be open for read and write access at the same time on different streams",
    "22.4" -> "There shall be no attempt to write to a stream which has been opened as read-only",
    "22.5" -> "A pointer to a FILE object shall not be dereferenced",
    "22.6" -> "The value of a pointer to a FILE shall not be used after the associated stream has been closed",
    "22.7" -> "The macro EOF shall only be compared with the unmodified return value from any Standard Library function capable of returning EOF",
    "22.8" -> "The value of errno shall be set to zero prior to a call to an errno-setting-function",
    "22.9" -> "The value of errno shall be tested against zero after calling an errno-setting-function",
    "22.10" -> "The value of errno shall only be tested when the last function to be called was an errno-setting-function"
  )


  private def generatePatterns(rules: Seq[Ruleset]): JsArray = {
    val defaultPatterns = Set(
      "AssignmentAddressToInteger",
      "AssignmentIntegerToAddress",
      "CastAddressToIntegerAtReturn",
      "CastIntegerToAddressAtReturn",
      "ConfigurationNotChecked",
      "IOWithoutPositioning",
      "StlMissingComparison",
      "arithOperationsOnVoidPointer",
      "arrayIndexOutOfBounds",
      "arrayIndexOutOfBoundsCond",
      "arrayIndexThenCheck",
      "assertWithSideEffect",
      "assignBoolToPointer",
      "assignIfError",
      "assignmentInAssert",
      "autoVariables",
      "autovarInvalidDeallocation",
      "badBitmaskCheck",
      "boostForeachError",
      "bufferAccessOutOfBounds",
      "catchExceptionByValue",
      "charBitOp",
      "charLiteralWithCharPtrCompare",
      "checkCastIntToCharAndBack",
      "clarifyCalculation",
      "clarifyCondition",
      "clarifyStatement",
      "commaSeparatedReturn",
      "compareBoolExpressionWithInt",
      "comparisonError",
      "comparisonFunctionIsAlwaysTrueOrFalse",
      "comparisonOfBoolWithBoolError",
      "comparisonOfFuncReturningBoolError",
      "comparisonOfTwoFuncsReturningBoolError",
      "constStatement",
      "copyCtorPointerCopying",
      "coutCerrMisusage",
      "cstyleCast",
      "deallocDealloc",
      "deallocret",
      "deallocuse",
      "derefInvalidIterator",
      "doubleFree",
      "duplInheritedMember",
      "duplicateBreak",
      "duplicateExpression",
      "duplicateExpressionTernary",
      "eraseDereference",
      "exceptDeallocThrow",
      "exceptRethrowCopy",
      "exceptThrowInDestructor",
      "fflushOnInputStream",
      "ignoredReturnValue",
      "incorrectLogicOperator",
      "incorrectStringBooleanError",
      "incorrectStringCompare",
      "incrementboolean",
      "integerOverflow",
      "invalidFunctionArg",
      "invalidFunctionArgBool",
      "invalidIterator1",
      "invalidLengthModifierError",
      "invalidPointerCast",
      "invalidPrintfArgType_float",
      "invalidPrintfArgType_n",
      "invalidPrintfArgType_p",
      "invalidPrintfArgType_s",
      "invalidPrintfArgType_sint",
      "invalidPrintfArgType_uint",
      "invalidScanfArgType_float",
      "invalidScanfArgType_int",
      "invalidScanfArgType_s",
      "invalidScanfFormatWidth",
      "invalidscanf",
      "knownConditionTrueFalse",
      "leakNoVarFunctionCall",
      "leakReturnValNotUsed",
      "literalWithCharPtrCompare",
      "mallocOnClassError",
      "mallocOnClassWarning",
      "memleak",
      "memleakOnRealloc",
      "memsetClass",
      "memsetClassFloat",
      "memsetClassReference",
      "memsetFloat",
      "memsetValueOutOfRange",
      "memsetZeroBytes",
      "mismatchAllocDealloc",
      "mismatchSize",
      "mismatchingBitAnd",
      "mismatchingContainers",
      "missingInclude",
      "missingIncludeSystem",
      "moduloAlwaysTrueFalse",
      "multiCondition",
      "nanInArithmeticExpression",
      "negativeIndex",
      "noConstructor",
      "noCopyConstructor",
      "noExplicitConstructor",
      "nullPointer",
      "nullPointerDefaultArg",
      "nullPointerRedundantCheck",
      "operatorEq",
      "operatorEqMissingReturnStatement",
      "operatorEqRetRefThis",
      "operatorEqShouldBeLeftUnimplemented",
      "operatorEqToSelf",
      "operatorEqVarError",
      "oppositeInnerCondition",
      "passedByValue",
      "pointerArithBool",
      "pointerLessThanZero",
      "pointerOutOfBounds",
      "pointerPositive",
      "pointerSize",
      "postfixOperator",
      "preprocessorErrorDirective",
      "publicAllocationError",
      "raceAfterInterlockedDecrement",
      "readWriteOnlyFile",
      "redundantAssignInSwitch",
      "redundantAssignment",
      "redundantCondition",
      "redundantCopy",
      "redundantCopyInSwitch",
      "redundantIfRemove",
      "redundantPointerOp",
      "resourceLeak",
      "returnAddressOfAutoVariable",
      "returnAddressOfFunctionParameter",
      "returnLocalVariable",
      "returnReference",
      "returnTempReference",
      "seekOnAppendedFile",
      "selfAssignment",
      "selfInitialization",
      "shiftNegative",
      "shiftTooManyBits",
      "signConversion",
      "sizeofCalculation",
      "sizeofDereferencedVoidPointer",
      "sizeofDivisionMemfunc",
      "sizeofVoid",
      "sizeofsizeof",
      "sizeofwithnumericparameter",
      "sizeofwithsilentarraypointer",
      "sprintfOverlappingData",
      "staticStringCompare",
      "stlBoundaries",
      "stlIfFind",
      "stlIfStrFind",
      "stlOutOfBounds",
      "stlSize",
      "stlcstr",
      "stlcstrParam",
      "stlcstrReturn",
      "strPlusChar",
      "stringCompare",
      "stringLiteralWrite",
      "thisSubtraction",
      "throwInNoexceptFunction",
      "toomanyconfigs",
      "truncLongCastAssignment",
      "truncLongCastReturn",
      "unassignedVariable",
      "uninitMemberVar",
      "uninitStructMember",
      "uninitdata",
      "uninitstring",
      "uninitvar",
      "unpreciseMathCall",
      "unreachableCode",
      "unreadVariable",
      "unsafeClassCanLeak",
      "unsignedLessThanZero",
      "unsignedPositive",
      "unusedAllocatedMemory",
      "unusedLabel",
      "unusedScopedObject",
      "unusedStructMember",
      "useClosedFile",
      "useInitializationList",
      "uselessAssignmentArg",
      "uselessAssignmentPtrArg",
      "uselessCallsCompare",
      "uselessCallsEmpty",
      "uselessCallsRemove",
      "uselessCallsSubstr",
      "uselessCallsSwap",
      "va_end_missing",
      "va_list_usedBeforeStarted",
      "va_start_referencePassed",
      "va_start_subsequentCalls",
      "va_start_wrongParameter",
      "varFuncNullUB",
      "variableScope",
      "virtualDestructor",
      "writeReadOnlyFile",
      "wrongPipeParameterSize",
      "wrongPrintfScanfArgNum",
      "wrongPrintfScanfParameterPositionError",
      "wrongmathcall",
      "zerodiv",
      "zerodivcond"
    )

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

      Json.obj(
        "patternId" -> rule.patternId,
        "level" -> level,
        "category" -> category,
        "enabled" -> defaultPatterns.contains(rule.patternId)
      )

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

  private def getRules(file: java.io.File): Seq[Ruleset] = {
    val outputXML: Elem = XML.loadFile(file)
    (outputXML \\ "errors" \\ "error").map { r =>
      Ruleset((r \ "@id").text, (r \ "@severity").text, (r \ "@msg").text, (r \ "@verbose").text)
    }
  }

  private def createPatternsAndDescriptionFile(rules: Seq[DocGenerator.Ruleset]): Unit = {
    val repoRoot: files.File = File(".")
    val docsRoot: files.File = File(repoRoot, "docs")
    val patternsFile: files.File = File(docsRoot, "patterns.json")
    val descriptionsRoot: files.File = File(docsRoot, "description")
    val descriptionsFile: files.File =
      File(descriptionsRoot, "description.json")

    val patterns: String = getPatterns(rules)
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
        "title" -> Json.toJsFieldJsValueWrapper(misraTitles(rule)),
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

  private def getPatterns(rules: Seq[DocGenerator.Ruleset]): String = {
    Json.prettyPrint(
      Json.obj(
        "name" -> "cppcheck",
        "version" -> Versions.cppcheckVersion,
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
