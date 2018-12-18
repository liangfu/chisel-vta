// See LICENSE.txt for license details.
package vta

import chisel3.iotesters.{Driver, TesterOptionsManager}
import utils.TutorialRunner
import freechips.rocketchip.config.Parameters

object Launcher {
  val examples = Map(
      "ALUSimple" -> { (manager: TesterOptionsManager) =>
        Driver.execute(() => new ALUSimple(), manager) {
          (c) => new ALUTests(c)
        }
      }
      // "Stack" -> { (manager: TesterOptionsManager) =>
      //   Driver.execute(() => new Stack(8), manager) {
      //     (c) => new StackTests(c)
      //   }
      // }
  )
  def main(args: Array[String]): Unit = {
    TutorialRunner("examples", examples, args)
  }
}

