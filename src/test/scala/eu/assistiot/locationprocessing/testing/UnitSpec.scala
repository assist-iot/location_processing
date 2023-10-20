package eu.assistiot.locationprocessing.testing

import org.scalatest.Inside
import org.scalatest.Inspectors
import org.scalatest.OptionValues
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

abstract class UnitSpec
    extends AnyFunSuite
    with Matchers
    with OptionValues
    with TryValues
    with Inside
    with Inspectors
    with ScalaCheckPropertyChecks
