package eu.assistiot.locationprocessing.v1.executor.parameters

import com.jayway.jsonpath.JsonPath

import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try

sealed trait Parameter
sealed trait StringParameter extends Parameter
sealed trait JsonParameter extends Parameter
case object InputTopic extends StringParameter
case object StringInputJson extends StringParameter
case object StringOutputJson extends StringParameter
final case class InputJson(jsonPath: JsonPath) extends JsonParameter
final case class OutputJson(jsonPath: JsonPath) extends JsonParameter

object ParameterParser {
  val singleQuote: Char = '\''
  val doubleQuote: Char = '"'
  val escapeCharacter: Char = '\\'
  val expressionStart: Char = '{'
  val expressionEnd: Char = '}'
  val inputTopic: String = "inputTopic"
  val inputJson: String = "input"
  val outputJson: String = "output"
  val stringInputJson: String = "strInput"
  val stringOutputJson: String = "strOutput"
  val jsonPathRoot: Char = '$'

  enum State {
    case Initial
    case InSingleQuoteString
    case InDoubleQuoteString
    case InExpression
    case InParameterAccessEnd
  }
}

class ParameterParser(isInputAvailable: Boolean, isOutputAvailable: Boolean) {
  import ParameterParser._

  @tailrec
  private def parse(
      str: String,
      state: State,
      inputPart: String,
      inputParts: List[String],
      params: List[Parameter]
  ): Try[Parsed] = {
    inline def next(
        str: String = str.tail,
        state: State = state,
        inputPart: String = inputPart,
        inputParts: List[String] = inputParts,
        params: List[Parameter] = params
    ): Try[Parsed] =
      parse(str, state, inputPart, inputParts, params)

    inline def failure(c: Char, msg: String): Try[Parsed] =
      Failure[Parsed](ParserException(state, c, str, inputPart, inputParts, params, msg))

    str.headOption match {
      case Some(c) =>
        state match {
          case State.Initial =>
            c match {
              case ParameterParser.expressionStart =>
                next(
                  state = State.InExpression,
                  inputPart = "",
                  inputParts = inputParts ++ List(inputPart)
                )
              case ParameterParser.singleQuote =>
                next(state = State.InSingleQuoteString, inputPart = inputPart + c.toString)
              case ParameterParser.doubleQuote =>
                next(state = State.InDoubleQuoteString, inputPart = inputPart + c.toString)
              case _ =>
                next(inputPart = inputPart + c.toString)
            }

          case State.InSingleQuoteString =>
            c match {
              case ParameterParser.singleQuote =>
                next(state = State.Initial, inputPart = inputPart + c.toString)
              case ParameterParser.escapeCharacter =>
                next(str = str.tail.tail, inputPart = inputPart + c.toString + str.tail)
              case _ =>
                next(inputPart = inputPart + c.toString)
            }

          case State.InDoubleQuoteString =>
            c match {
              case ParameterParser.doubleQuote =>
                next(state = State.Initial, inputPart = inputPart + c.toString)
              case ParameterParser.escapeCharacter =>
                next(str = str.tail.tail, inputPart = inputPart + c.toString + str.tail)
              case _ =>
                next(inputPart = inputPart + c.toString)
            }

          case State.InExpression =>
            if (str.startsWith(inputTopic)) {
              if (!isInputAvailable) {
                failure(c, "input topic is not available")
              } else {
                next(
                  str = str.substring(inputTopic.length),
                  state = State.InParameterAccessEnd,
                  params = params ++ List(InputTopic)
                )
              }
            } else if (str.startsWith(inputJson)) {
              if (!isInputAvailable) {
                failure(c, "input json is not available")
              } else {
                val indexOfExpressionEnd = str.indexOf(expressionEnd)
                if (indexOfExpressionEnd.equals(-1)) {
                  failure(c, "expression end not found")
                } else {
                  val expression =
                    jsonPathRoot.toString + str.slice(inputJson.length, indexOfExpressionEnd)
                  val jsonPath = Try(JsonPath.compile(expression))
                  jsonPath match {
                    case Success(path) =>
                      next(
                        str = str.substring(indexOfExpressionEnd),
                        state = State.InParameterAccessEnd,
                        params = params ++ List(InputJson(path))
                      )
                    case Failure(exception) =>
                      failure(c, exception.getMessage)
                  }
                }
              }
            } else if (str.startsWith(outputJson)) {
              if (!isOutputAvailable) {
                failure(c, "output json is not available")
              } else {
                val indexOfExpressionEnd = str.indexOf(expressionEnd)
                if (indexOfExpressionEnd.equals(-1)) {
                  failure(c, "expression end not found")
                } else {
                  val expression =
                    jsonPathRoot.toString + str.slice(outputJson.length, indexOfExpressionEnd)
                  val jsonPath = Try(JsonPath.compile(expression))
                  jsonPath match {
                    case Success(path) =>
                      next(
                        str = str.substring(indexOfExpressionEnd),
                        state = State.InParameterAccessEnd,
                        params = params ++ List(OutputJson(path))
                      )
                    case Failure(exception) =>
                      failure(c, exception.getMessage)
                  }
                }
              }
            } else if (str.startsWith(stringInputJson)) {
              if (!isInputAvailable) {
                failure(c, "input string json is not available")
              } else {
                next(
                  str = str.substring(stringInputJson.length),
                  state = State.InParameterAccessEnd,
                  params = params ++ List(StringInputJson)
                )
              }
            } else if (str.startsWith(stringOutputJson)) {
              if (!isOutputAvailable) {
                failure(c, "output string json is not available")
              } else {
                next(
                  str = str.substring(stringOutputJson.length),
                  state = State.InParameterAccessEnd,
                  params = params ++ List(StringOutputJson)
                )
              }
            } else if (c.isWhitespace) {
              next()
            } else {
              failure(c, "unknown value of expression")
            }

          case State.InParameterAccessEnd =>
            c match {
              case ParameterParser.expressionEnd =>
                next(state = State.Initial)
              case _ if c.isWhitespace =>
                next()
              case _ =>
                failure(c, "expression not closed")
            }
        }

      case None =>
        inputPart match {
          case "" if inputParts.nonEmpty =>
            Success(Parsed(inputParts, params))
          case _ =>
            Success(Parsed(inputParts ++ List(inputPart), params))
        }
    }
  }

  def run(str: String): Try[Parsed] = {
    parse(str, State.Initial, "", List[String](), List[Parameter]())
  }
}
