package eu.assistiot.locationprocessing.testing

import eu.assistiot.locationprocessing.v1.executor.parameters.InputJson
import eu.assistiot.locationprocessing.v1.executor.parameters.OutputJson
import eu.assistiot.locationprocessing.v1.executor.parameters.Parameter
import org.scalactic.Equality

object Equality {
  implicit val parameterEq: Equality[Parameter] =
    (left: Parameter, right: Any) =>
      (left, right) match {
        case (left: InputJson, right: InputJson) =>
          left.jsonPath.getPath.equals(right.jsonPath.getPath)
        case (left: OutputJson, right: OutputJson) =>
          left.jsonPath.getPath.equals(right.jsonPath.getPath)
        case (left, right: Parameter) =>
          left.equals(right)
        case _ => false
      }
}
