package eu.assistiot.locationprocessing.v1.executor.parameters

final case class ParserException(
    state: ParameterParser.State,
    observedValue: Char,
    str: String,
    inputPart: String,
    inputParts: List[String],
    params: List[Parameter],
    msg: String
) extends Exception(
      s"""\nInvalid state: \"$state\",
         |observed value: \"$observedValue\",
         |string: \"$str\",
         |input part: \"$inputPart\",
         |input parts: \"$inputParts\",
         |parameters: \"$params\",
         |msg: \"$msg\"""".stripMargin
    )
