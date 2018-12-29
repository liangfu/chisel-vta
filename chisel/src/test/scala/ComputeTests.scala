// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import chisel3.testers._
import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}

class ComputeTests(c: Compute)(implicit val p: freechips.rocketchip.config.Parameters)
    extends PeekPokeTester(c) {
  val xlen = p(XLEN)

  val insn0 = "h00000001000100010000000000000000".U
  val insn1 = "h00000008000800010000000000000180".U
  val insn2 = "h7ffe4002000008000010000800200044".U

  poke(c.io.gemm_queue.data, 0.U)
  poke(c.io.gemm_queue.valid, 0.U)
  poke(c.io.uops.data, 0.U)
  poke(c.io.uops.valid, 0.U)
  step(1)
  poke(c.io.gemm_queue.data, insn0)
  poke(c.io.gemm_queue.valid, 1.U)
  poke(c.io.uops.data, "h4000".U)
  poke(c.io.uops.valid, 1.U)
  step(1)
  expect(c.io.uops.ready, 1.U)
  poke(c.io.uops.valid, 0.U)
  poke(c.io.gemm_queue.data, insn1)
  poke(c.io.gemm_queue.valid, 1.U)
  poke(c.io.biases.valid, 1.U)
  poke(c.io.biases.data, "h0000".U)
  step(1)
  expect(c.io.biases.ready, 1.U)
  poke(c.io.gemm_queue.data, insn2)
  poke(c.io.biases.data, "h1111".U)
  step(1)
  poke(c.io.biases.data, "h2222".U)
  step(1)
  poke(c.io.biases.data, "h3333".U)
  step(1)
  poke(c.io.biases.data, "h4444".U)
  step(1)
  poke(c.io.biases.data, "h5555".U)
  step(1)
  poke(c.io.biases.data, "h6666".U)
  step(1)
  poke(c.io.biases.data, "h7777".U)
  step(1)
  poke(c.io.biases.valid, 0.U)
  poke(c.io.gemm_queue.data, insn2)
  poke(c.io.gemm_queue.valid, 1.U)
  step(1)
  step(1)
  step(1)
  step(1)
  step(1)
  step(1)
  step(1)
  poke(c.io.gemm_queue.data, 0.U)
  poke(c.io.gemm_queue.valid, 0.U)
  step(1)
}

class ComputeTester extends ChiselFlatSpec {
  implicit val p = (new VTAConfig).toInstance
  behavior of "Compute"
  backends foreach {backend =>
    it should s"perform correct math operation on dynamic operand in $backend" in {
      Driver(() => new Compute(), backend)((c) => new ComputeTests(c)) should be (true)
    }
  }
}

