package indigoextras.subsystems

import indigo.shared.subsystems.SubSystem
import indigo.shared.datatypes.Point
import indigo.shared.datatypes.FontKey
import indigo.shared.time.Seconds
import indigo.shared.events.GlobalEvent
import indigo.shared.subsystems.SubSystemFrameContext
import indigo.shared.Outcome
import indigo.shared.scenegraph.SceneUpdateFragment
import indigo.shared.datatypes.RGBA
import indigo.shared.scenegraph.Text
import indigo.shared.events.FrameTick

object FPSCounter {

  def apply(fontKey: FontKey, position: Point, targetFPS: Int): SubSystem =
    SubSystem[GlobalEvent, FPSCounterState](
      _eventFilter = eventFilter,
      _initialModel = FPSCounterState.default,
      _update = update(targetFPS),
      _present = present(fontKey, position, targetFPS)
    )

  lazy val eventFilter: GlobalEvent => Option[GlobalEvent] = {
    case FrameTick => Option(FrameTick)
    case _         => None
  }

  def update(targetFPS: Int): (SubSystemFrameContext, FPSCounterState) => GlobalEvent => Outcome[FPSCounterState] =
    (frameContext, model) => {
      case FrameTick =>
        if (frameContext.gameTime.running >= (model.lastInterval + Seconds(1)))
          Outcome(
            FPSCounterState(
              fps = Math.min(targetFPS, model.frameCountSinceInterval + 1),
              lastInterval = frameContext.gameTime.running,
              frameCountSinceInterval = 0
            )
          )
        else
          Outcome(model.copy(frameCountSinceInterval = model.frameCountSinceInterval + 1))
    }

  def present(fontKey: FontKey, position: Point, targetFPS: Int): (SubSystemFrameContext, FPSCounterState) => SceneUpdateFragment =
    (_, model) => {
      SceneUpdateFragment.empty
        .addUiLayerNodes(
          Text(s"""FPS ${model.fps.toString}""", position.x, position.y, 1, fontKey)
            .withTint(pickTint(targetFPS, model.fps))
        )
    }

  def pickTint(targetFPS: Int, fps: Int): RGBA =
    if (fps > targetFPS - (targetFPS * 0.05)) RGBA.Green
    else if (fps > targetFPS / 2) RGBA.Yellow
    else RGBA.Red

}

final case class FPSCounterState(fps: Int, lastInterval: Seconds, frameCountSinceInterval: Int)
object FPSCounterState {
  def default: FPSCounterState =
    FPSCounterState(0, Seconds.zero, 0)
}
