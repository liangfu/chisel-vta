// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import chisel3.testers._
import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}

class ComputeTests(c: ComputeSimple)(implicit val p: freechips.rocketchip.config.Parameters) extends PeekPokeTester(c) {
  val xlen = p(XLEN)
  for (n <- 0 until 64) {
  }
}

class ComputeTester extends ChiselFlatSpec {
  implicit val p = (new VTAConfig).toInstance
  behavior of "ComputeSimple"
  backends foreach {backend =>
    it should s"perform correct math operation on dynamic operand in $backend" in {
      Driver(() => new ComputeSimple(), backend)((c) => new ComputeTests(c)) should be (true)
    }
  }
}

