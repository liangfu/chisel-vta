// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.{Parameters, Field}

class StoreIO(implicit p: Parameters) extends CoreBundle()(p) {
  val outputs = Flipped(new AvalonSinkIO(dataBits = 128))
  val store_queue = new AvalonSinkIO(dataBits = 128)
  val s2g_dep_queue = Flipped(new AvalonSinkIO(dataBits = 1))
  val g2s_dep_queue = new AvalonSinkIO(dataBits = 1)
  val out_mem = Flipped(new AvalonSlaveIO(dataBits = 128, addrBits = 17))
}

class Store(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new StoreIO())

  val insn            = Reg(UInt(128.W))
  val insn_valid      = insn =/= 0.U

  // Decode
  val pop_prev_dependence = insn(insn_mem_1)
  val pop_next_dependence = insn(insn_mem_2)
  val push_prev_dependence = insn(insn_mem_3)
  val push_next_dependence = insn(insn_mem_4)
  val memory_type = insn(insn_mem_5_1, insn_mem_5_0)
  val sram_base   = insn(insn_mem_6_1, insn_mem_6_0)
  val dram_base   = insn(insn_mem_7_1, insn_mem_7_0)
  val y_size      = insn(insn_mem_8_1, insn_mem_8_0)
  val x_size      = insn(insn_mem_9_1, insn_mem_9_0)
  val x_stride    = insn(insn_mem_a_1, insn_mem_a_0)
  val y_pad_0     = insn(insn_mem_b_1, insn_mem_b_0)
  val y_pad_1     = insn(insn_mem_c_1, insn_mem_c_0)
  val x_pad_0     = insn(insn_mem_d_1, insn_mem_d_0)
  val x_pad_1     = insn(insn_mem_e_1, insn_mem_e_0)

  val y_size_total = y_pad_0 + y_size + y_pad_1
  val x_size_total = x_pad_0 + x_size + x_pad_1
  val y_offset = x_size_total * y_pad_0

  val sram_idx = (sram_base + y_offset) + x_pad_0
  val dram_idx = dram_base

  // counters
  val acc_x_cntr_max = 8
  val acc_x_cntr_en = RegNext(!io.out_mem.waitrequest && io.outputs.valid) /// ????
  val (acc_x_cntr_val, acc_x_cntr_wrap) = Counter(acc_x_cntr_en, acc_x_cntr_max)

  // io.store_queue <> DontCare
  // io.outputs <> DontCare
  io.s2g_dep_queue <> DontCare
  io.g2s_dep_queue <> DontCare
  // io.out_mem <> DontCare

  // fetch instruction
  when (io.store_queue.valid) {
    insn := io.store_queue.data
    io.store_queue.ready := 1.U
  } .otherwise {
    insn := insn
    io.store_queue.ready := 0.U
  }

  val out_mem_addr = RegNext(acc_x_cntr_val)
  io.out_mem.address := out_mem_addr
  io.outputs.data := RegNext(io.out_mem.readdata)
  io.out_mem.write := 0.U
  io.out_mem.writedata := 0.U

  when (acc_x_cntr_en) {
    io.out_mem.read := 1.U
    io.outputs.valid := 1.U
  } .otherwise {
    io.outputs.valid := 0.U
    io.out_mem.read := 0.U
  }
}

