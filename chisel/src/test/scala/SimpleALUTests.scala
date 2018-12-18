// See LICENSE.txt for license details.
package examples

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}

class SimpleALUTests(c: SimpleALU) extends PeekPokeTester(c) {
  for (n <- 0 until 64) {
    val a      = rnd.nextInt(127)
    val b      = rnd.nextInt(127)
    val opcode = rnd.nextInt(4)
    var output = 0
    if (opcode == 0) {
      output = if (a < b) a else b
    } else if (opcode == 1) {
      output = if (a < b) b else a
    } else if (opcode == 2) {
      output = a + b
    } else {
      output = a >> 1
    }
    poke(c.io.A, a)
    poke(c.io.B, b)
    poke(c.io.opcode, opcode)
    step(1)
    expect(c.io.out, output)
  }
}

class SimpleALUTester extends ChiselFlatSpec {
  behavior of "SimpleALU"
  backends foreach {backend =>
    it should s"perform correct math operation on dynamic operand in $backend" in {
      Driver(() => new SimpleALU(), backend)((c) => new SimpleALUTests(c)) should be (true)
    }
  }
}

