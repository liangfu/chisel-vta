// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import chisel3.testers._
import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}

class StoreTests(c: Store)(implicit val p: freechips.rocketchip.config.Parameters)
    extends PeekPokeTester(c) {

  val insn0 = "h00000008000800010000000000000029".U

  step(1)
  poke(c.io.store_queue.data, insn0)
  poke(c.io.store_queue.valid, 1.U)
  step(1)
  poke(c.io.store_queue.valid, 0.U)
  step(1)
  step(1)

}

class StoreTester extends ChiselFlatSpec {
  implicit val p = (new VTAConfig).toInstance
  behavior of "Store"
  backends foreach {backend =>
    it should s"perform correct math operation on dynamic operand in $backend" in {
      Driver(() => new Store(), backend)((c) => new StoreTests(c)) should be (true)
    }
  }
}

