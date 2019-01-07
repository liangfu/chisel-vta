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
  step(1)
  poke(c.io.biases.valid, 1.U)
  poke(c.io.biases.data, "hfffffff6ffffffc4ffffffc7ffffffefffffffc1ffffffecfffffff30000001e000000040000000a0000000b00000039ffffffeeffffffc6ffffffe9fffffff5".U)
  poke(c.io.gemm_queue.data, insn2)
  step(1)
  expect(c.io.biases.ready, 1.U)
  poke(c.io.biases.data, "h000000120000003afffffffb00000010fffffff3ffffffde00000034fffffffa0000002a00000037ffffffcd00000022ffffffd3ffffffc60000003400000020".U)
  step(1)
  poke(c.io.biases.data, "h000000310000003efffffffcffffffd800000031000000170000000b0000003effffffd10000002200000038ffffffd0ffffffdbfffffff8ffffffc7ffffffc7".U)
  step(1)
  poke(c.io.biases.data, "hffffffe20000003c00000023ffffffddffffffdb00000016000000200000000f0000002bffffffd9ffffffc5fffffff800000007fffffff8ffffffec00000031".U)
  step(1)
  poke(c.io.biases.data, "hfffffff8ffffffe10000000afffffff30000001400000017ffffffdcffffffd300000028000000290000002e0000000f00000018000000250000002bffffffe3".U)
  step(1)
  poke(c.io.biases.data, "h000000020000001b00000009000000300000003b00000016ffffffe90000002dfffffffa0000001fffffffe9ffffffca0000001000000019ffffffce00000021".U)
  step(1)
  poke(c.io.biases.data, "hffffffd3000000160000003affffffe7fffffff2ffffffd20000002e000000030000001100000008000000320000001d0000002f00000034ffffffdefffffff1".U)
  step(1)
  poke(c.io.biases.data, "hfffffffa0000003ffffffff50000002f00000019ffffffc8ffffffd6fffffffa0000001ffffffff1000000010000003efffffff6000000110000002400000017".U)
  step(1)
  poke(c.io.biases.valid, 0.U)
  poke(c.io.gemm_queue.data, insn2)
  poke(c.io.gemm_queue.valid, 1.U)
  expect(c.io.out_mem.writedata, "hf6c4c7efc1ecf3fcfcfcfcfceec6e9f5".U)
  step(1)
  poke(c.io.gemm_queue.data, 0.U)
  poke(c.io.gemm_queue.valid, 1.U)
  expect(c.io.out_mem.writedata, "hfcfcfbfcf3defcfafcfccdfcd3c6fcfc".U)
  step(1)
  expect(c.io.out_mem.writedata, "hfcfcfcd8fcfcfcfcd1fcfcd0dbf8c7c7".U)
  step(1)
  expect(c.io.out_mem.writedata, "he2fcfcdddbfcfcfcfcd9c5f8fcf8ecfc".U)
  step(1)
  step(1)
  step(1)
  step(1)
  step(1)
  step(1)
  step(1)

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
  step(1)
  poke(c.io.biases.valid, 1.U)
  poke(c.io.biases.data, "hfffffff6ffffffc4ffffffc7ffffffefffffffc1ffffffecfffffff30000001e000000040000000a0000000b00000039ffffffeeffffffc6ffffffe9fffffff5".U)
  poke(c.io.gemm_queue.data, insn2)
  step(1)
  expect(c.io.biases.ready, 1.U)
  poke(c.io.biases.data, "h000000120000003afffffffb00000010fffffff3ffffffde00000034fffffffa0000002a00000037ffffffcd00000022ffffffd3ffffffc60000003400000020".U)
  step(1)
  poke(c.io.biases.data, "h000000310000003efffffffcffffffd800000031000000170000000b0000003effffffd10000002200000038ffffffd0ffffffdbfffffff8ffffffc7ffffffc7".U)
  step(1)
  poke(c.io.biases.data, "hffffffe20000003c00000023ffffffddffffffdb00000016000000200000000f0000002bffffffd9ffffffc5fffffff800000007fffffff8ffffffec00000031".U)
  step(1)
  poke(c.io.biases.data, "hfffffff8ffffffe10000000afffffff30000001400000017ffffffdcffffffd300000028000000290000002e0000000f00000018000000250000002bffffffe3".U)
  step(1)
  poke(c.io.biases.data, "h000000020000001b00000009000000300000003b00000016ffffffe90000002dfffffffa0000001fffffffe9ffffffca0000001000000019ffffffce00000021".U)
  step(1)
  poke(c.io.biases.data, "hffffffd3000000160000003affffffe7fffffff2ffffffd20000002e000000030000001100000008000000320000001d0000002f00000034ffffffdefffffff1".U)
  step(1)
  poke(c.io.biases.data, "hfffffffa0000003ffffffff50000002f00000019ffffffc8ffffffd6fffffffa0000001ffffffff1000000010000003efffffff6000000110000002400000017".U)
  step(1)
  poke(c.io.biases.valid, 0.U)
  poke(c.io.gemm_queue.data, insn2)
  poke(c.io.gemm_queue.valid, 1.U)
  expect(c.io.out_mem.writedata, "hf6c4c7efc1ecf3fcfcfcfcfceec6e9f5".U)
  step(1)
  poke(c.io.gemm_queue.data, 0.U)
  poke(c.io.gemm_queue.valid, 1.U)
  expect(c.io.out_mem.writedata, "hfcfcfbfcf3defcfafcfccdfcd3c6fcfc".U)
  step(1)
  expect(c.io.out_mem.writedata, "hfcfcfcd8fcfcfcfcd1fcfcd0dbf8c7c7".U)
  step(1)
  expect(c.io.out_mem.writedata, "he2fcfcdddbfcfcfcfcd9c5f8fcf8ecfc".U)
  step(1)
  step(1)
  step(1)
  step(1)
  step(1)
  step(1)
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

