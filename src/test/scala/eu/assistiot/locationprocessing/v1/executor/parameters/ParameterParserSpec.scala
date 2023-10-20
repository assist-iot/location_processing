package eu.assistiot.locationprocessing.v1.executor.parameters

import com.jayway.jsonpath.JsonPath
import eu.assistiot.locationprocessing.testing.Equality.parameterEq
import eu.assistiot.locationprocessing.testing.UnitSpec
import eu.assistiot.locationprocessing.v1.executor.parameters.InputJson
import eu.assistiot.locationprocessing.v1.executor.parameters.OutputJson
import eu.assistiot.locationprocessing.v1.executor.parameters.Parameter
import eu.assistiot.locationprocessing.v1.executor.parameters.ParameterParser
import eu.assistiot.locationprocessing.v1.executor.parameters.Parsed
import eu.assistiot.locationprocessing.v1.executor.parameters.ParserException
import eu.assistiot.locationprocessing.v1.executor.parameters.StringInputJson
import eu.assistiot.locationprocessing.v1.executor.parameters.StringOutputJson
import org.scalatest.matchers.dsl.ResultOfATypeInvocation
import org.scalatest.prop.TableFor3

import scala.util.Try

class ParameterParserSpec extends UnitSpec {
  val parsers: TableFor3[ParameterParser, Boolean, Boolean] = Table(
    ("parser", "isInputAvailable", "isOutputAvailable"),
    (ParameterParser(isInputAvailable = true, isOutputAvailable = true), true, true),
    (ParameterParser(isInputAvailable = true, isOutputAvailable = false), true, false),
    (ParameterParser(isInputAvailable = false, isOutputAvailable = true), false, true),
    (ParameterParser(isInputAvailable = false, isOutputAvailable = false), false, false)
  )

  def paramCombination(isInputAvailable: Boolean, isOutputAvailable: Boolean): String =
    "input is " +
      s"${if (isInputAvailable) "available" else "not available"} " +
      "and output is " +
      s"${if (isOutputAvailable) "available" else "not available"}"

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      test(
        s"Returns input parts with an empty string and no parameters for empty string when ${paramCombination(isInputAvailable, isOutputAvailable)}"
      ) {
        val input: String = ""

        val expectedInputParts: List[String] = List[String](input)
        val expectedParameters: List[Parameter] = List[Parameter]()
        val expected: Parsed = Parsed(expectedInputParts, expectedParameters)

        val result: Try[Parsed] = parser.run(input)
        result.success.value.inputParts should contain theSameElementsInOrderAs expected.inputParts
        result.success.value.parameters should contain theSameElementsInOrderAs expected.parameters
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      whenever(!isInputAvailable) {
        test(
          s"Fails if input topic is used when ${paramCombination(isInputAvailable, isOutputAvailable)}"
        ) {
          val input: String =
            ParameterParser.expressionStart.toString +
              ParameterParser.inputTopic +
              ParameterParser.expressionEnd.toString

          val expected: ResultOfATypeInvocation[ParserException] = a[ParserException]

          val result: Try[Parsed] = parser.run(input)
          result.failure.exception shouldBe expected
        }
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      whenever(!isInputAvailable) {
        test(
          s"Fails if input json is used when ${paramCombination(isInputAvailable, isOutputAvailable)}"
        ) {
          val input: String =
            ParameterParser.expressionStart.toString +
              ParameterParser.inputJson +
              ParameterParser.expressionEnd.toString

          val expected: ResultOfATypeInvocation[ParserException] = a[ParserException]

          val result: Try[Parsed] = parser.run(input)
          result.failure.exception shouldBe expected
        }
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      whenever(!isOutputAvailable) {
        test(
          s"Fails if output json is used when ${paramCombination(isInputAvailable, isOutputAvailable)}"
        ) {
          val input: String =
            ParameterParser.expressionStart.toString +
              ParameterParser.outputJson +
              ParameterParser.expressionEnd.toString

          val expected: ResultOfATypeInvocation[ParserException] = a[ParserException]

          val result: Try[Parsed] = parser.run(input)
          result.failure.exception shouldBe expected
        }
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      whenever(!isInputAvailable) {
        test(
          s"Fails if string input json is used when ${paramCombination(isInputAvailable, isOutputAvailable)}"
        ) {
          val input: String =
            ParameterParser.expressionStart.toString +
              ParameterParser.stringInputJson +
              ParameterParser.expressionEnd.toString

          val expected: ResultOfATypeInvocation[ParserException] = a[ParserException]

          val result: Try[Parsed] = parser.run(input)
          result.failure.exception shouldBe expected
        }
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      whenever(!isOutputAvailable) {
        test(
          s"Fails if string output json is used when ${paramCombination(isInputAvailable, isOutputAvailable)}"
        ) {
          val input: String =
            ParameterParser.expressionStart.toString +
              ParameterParser.stringOutputJson +
              ParameterParser.expressionEnd.toString

          val expected: ResultOfATypeInvocation[ParserException] = a[ParserException]

          val result: Try[Parsed] = parser.run(input)
          result.failure.exception shouldBe expected
        }
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      whenever(isInputAvailable) {
        test(
          s"Allows to use input topic when ${paramCombination(isInputAvailable, isOutputAvailable)}"
        ) {
          val input: String =
            ParameterParser.expressionStart.toString +
              ParameterParser.inputTopic +
              ParameterParser.expressionEnd.toString

          val expectedInputParts: List[String] = List[String]("")
          val expectedParameters: List[Parameter] = List[Parameter](InputTopic)
          val expected: Parsed = Parsed(expectedInputParts, expectedParameters)

          val result: Try[Parsed] = parser.run(input)
          result.success.value.inputParts should contain theSameElementsInOrderAs expected.inputParts
          result.success.value.parameters should contain theSameElementsInOrderAs expected.parameters
        }
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      whenever(isInputAvailable) {
        test(
          s"Allows to use input json when ${paramCombination(isInputAvailable, isOutputAvailable)}"
        ) {
          val input: String =
            ParameterParser.expressionStart.toString +
              ParameterParser.inputJson +
              ParameterParser.expressionEnd.toString

          val expectedInputParts: List[String] = List[String]("")
          val expectedParameters: List[Parameter] =
            List[Parameter](InputJson(JsonPath.compile(ParameterParser.jsonPathRoot.toString)))
          val expected: Parsed = Parsed(expectedInputParts, expectedParameters)

          val result: Try[Parsed] = parser.run(input)
          result.success.value.inputParts should contain theSameElementsInOrderAs expected.inputParts
          result.success.value.parameters should contain theSameElementsInOrderAs expected.parameters
        }
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      whenever(isOutputAvailable) {
        test(
          s"Allows to use output json when ${paramCombination(isInputAvailable, isOutputAvailable)}"
        ) {
          val input: String =
            ParameterParser.expressionStart.toString +
              ParameterParser.outputJson +
              ParameterParser.expressionEnd.toString

          val expectedInputParts: List[String] = List[String]("")
          val expectedParameters: List[Parameter] =
            List[Parameter](OutputJson(JsonPath.compile(ParameterParser.jsonPathRoot.toString)))
          val expected: Parsed = Parsed(expectedInputParts, expectedParameters)

          val result: Try[Parsed] = parser.run(input)
          result.success.value.inputParts should contain theSameElementsInOrderAs expected.inputParts
          result.success.value.parameters should contain theSameElementsInOrderAs expected.parameters
        }
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      whenever(isInputAvailable) {
        test(
          s"Allows to use string input json when ${paramCombination(isInputAvailable, isOutputAvailable)}"
        ) {
          val input: String =
            ParameterParser.expressionStart.toString +
              ParameterParser.stringInputJson +
              ParameterParser.expressionEnd.toString

          val expectedInputParts: List[String] = List[String]("")
          val expectedParameters: List[Parameter] = List[Parameter](StringInputJson)
          val expected: Parsed = Parsed(expectedInputParts, expectedParameters)

          val result: Try[Parsed] = parser.run(input)
          result.success.value.inputParts should contain theSameElementsInOrderAs expected.inputParts
          result.success.value.parameters should contain theSameElementsInOrderAs expected.parameters
        }
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      whenever(isOutputAvailable) {
        test(
          s"Allows to use string output json when ${paramCombination(isInputAvailable, isOutputAvailable)}"
        ) {
          val input: String =
            ParameterParser.expressionStart.toString +
              ParameterParser.stringOutputJson +
              ParameterParser.expressionEnd.toString

          val expectedInputParts: List[String] = List[String]("")
          val expectedParameters: List[Parameter] = List[Parameter](StringOutputJson)
          val expected: Parsed = Parsed(expectedInputParts, expectedParameters)

          val result: Try[Parsed] = parser.run(input)
          result.success.value.inputParts should contain theSameElementsInOrderAs expected.inputParts
          result.success.value.parameters should contain theSameElementsInOrderAs expected.parameters
        }
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      test(
        s"Omits parameters in single quoted strings when ${paramCombination(isInputAvailable, isOutputAvailable)}"
      ) {
        val exampleParameter: String = ParameterParser.inputJson
        val input: String =
          ParameterParser.singleQuote.toString +
            ParameterParser.expressionStart.toString +
            exampleParameter +
            ParameterParser.expressionEnd.toString +
            ParameterParser.singleQuote.toString

        val expectedInputParts: List[String] = List[String](input)
        val expectedParameters: List[Parameter] = List[Parameter]()
        val expected: Parsed = Parsed(expectedInputParts, expectedParameters)

        val result: Try[Parsed] = parser.run(input)
        result.success.value.inputParts should contain theSameElementsInOrderAs expected.inputParts
        result.success.value.parameters should contain theSameElementsInOrderAs expected.parameters
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      test(
        s"Omits parameters in double quoted strings when ${paramCombination(isInputAvailable, isOutputAvailable)}"
      ) {
        val exampleParameter: String = ParameterParser.stringInputJson
        val input: String =
          ParameterParser.doubleQuote.toString +
            ParameterParser.expressionStart.toString +
            exampleParameter +
            ParameterParser.expressionEnd.toString +
            ParameterParser.doubleQuote.toString

        val expectedInputParts: List[String] = List[String](input)
        val expectedParameters: List[Parameter] = List[Parameter]()
        val expected: Parsed = Parsed(expectedInputParts, expectedParameters)

        val result: Try[Parsed] = parser.run(input)
        result.success.value.inputParts should contain theSameElementsInOrderAs expected.inputParts
        result.success.value.parameters should contain theSameElementsInOrderAs expected.parameters
      }
  }

  forAll(parsers) {
    (parser: ParameterParser, isInputAvailable: Boolean, isOutputAvailable: Boolean) =>
      test(
        s"Ignores parameter name not in expression when ${paramCombination(isInputAvailable, isOutputAvailable)}"
      ) {
        val exampleParameter: String = ParameterParser.stringInputJson
        val input: String = exampleParameter

        val expectedInputParts: List[String] = List[String](input)
        val expectedParameter: List[Parameter] = List[Parameter]()
        val expected: Parsed = Parsed(expectedInputParts, expectedParameter)

        val result: Try[Parsed] = parser.run(input)
        result.success.value.inputParts should contain theSameElementsInOrderAs expected.inputParts
        result.success.value.parameters should contain theSameElementsInOrderAs expected.parameters
      }
  }
}
