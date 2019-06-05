package indigoexamples.model

import org.scalacheck._

import indigo.Dice
import ingidoexamples.model.LaunchPad
import indigo.shared.datatypes.Point
import indigo.EqualTo._
import indigo.shared.datatypes.Rectangle

class LaunchPadSpecification extends Properties("LaunchPad") {

  import Generators._

  def launchPadGen: Gen[LaunchPad] =
    for {
      dice   <- diceGen
      points <- pointsOnALineGen
    } yield LaunchPad.generateLaunchPad(dice, points.start, points.end, Rectangle.zero)

  property("generate a launch pad with a timer up to 1.5 seconds") = Prop.forAll(launchPadGen) { launchPad =>
    launchPad.countDown.value >= 1 && launchPad.countDown.value <= 1500
  }

  property("generate a launch pad point along the base line") = Prop.forAll(diceGen, pointsOnALineGen) { (dice, points) =>
    val launchPad: LaunchPad =
      LaunchPad.generateLaunchPad(dice, points.start, points.end, Rectangle.zero)

    launchPad.position.y === points.end.y && launchPad.position.x >= points.start.x && launchPad.position.x <= points.end.x
  }

}