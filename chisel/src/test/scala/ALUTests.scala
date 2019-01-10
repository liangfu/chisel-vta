// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import chisel3.testers._
import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import ALU._

class ALUTests(c: ALU)(implicit val p: freechips.rocketchip.config.Parameters) extends PeekPokeTester(c) {
  for (n <- 0 until 64) {
    val a      = rnd.nextInt(127)
    val b      = rnd.nextInt(127)
    val alu_op = rnd.nextInt(4)
    var output = 0
    
    if (alu_op == 0) {
      output = if (a < b) a else b
    } else if (alu_op == 1) {
      output = if (a < b) b else a
    } else if (alu_op == 2) {
      output = a + b
    } else {
      output = a >> b.toInt
    }
    poke(c.io.A, a)
    poke(c.io.B, b)
    poke(c.io.alu_op, alu_op)
    step(1)
    expect(c.io.out, output)
  }
}

class ALUTester extends ChiselFlatSpec {
  implicit val p = (new VTAConfig).toInstance
  behavior of "ALU"
  backends foreach {backend =>
    it should s"perform correct math operation on dynamic operand in $backend" in {
      Driver(() => new ALU(), backend)((c) => new ALUTests(c)) should be (true)
    }
  }
}

