// See LICENSE.txt for license details.
package vta

import chisel3.iotesters.{Driver, TesterOptionsManager}
import utils.TutorialRunner
import freechips.rocketchip.config.Parameters

object Launcher {
  implicit val p = (new VTAConfig).toInstance
  val modules = Map(
      "ALU" -> { (manager: TesterOptionsManager) =>
        Driver.execute(() => new ALU(), manager) {
          (c) => new ALUTests(c)
        }
      },
      "Compute" -> { (manager: TesterOptionsManager) =>
        Driver.execute(() => new Compute(), manager) {
          (c) => new ComputeTests(c)
        }
      }
  )
  def main(args: Array[String]): Unit = {
    TutorialRunner("vta", modules, args)
  }
}

