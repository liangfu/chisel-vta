// See LICENSE.txt for license details.
package vta

import chisel3.iotesters.{Driver, TesterOptionsManager}
import utils.TutorialRunner
import freechips.rocketchip.config.Parameters

object Launcher {
  implicit val p = (new VTAConfig).toInstance
  val modules = Map(
      "ALUSimple" -> { (manager: TesterOptionsManager) =>
        Driver.execute(() => new ALUSimple(), manager) {
          (c) => new ALUTests(c)
        }
      },
      "ComputeSimple" -> { (manager: TesterOptionsManager) =>
        Driver.execute(() => new ComputeSimple(), manager) {
          (c) => new ComputeTests(c)
        }
      }
  )
  def main(args: Array[String]): Unit = {
    TutorialRunner("vta", modules, args)
  }
}

