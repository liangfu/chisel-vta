// See LICENSE for license details.

package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters

object Control {
  val Y = true.B
  val N = false.B

  import ALU._
}

class ControlSignals(implicit p: Parameters) extends CoreBundle()(p) {
  val done = new AvalonSlaveIO(dataBits = 1, addrBits = 1)
  val uops = new AvalonSourceIO(dataBits = 32)
  val biases = new AvalonSourceIO(dataBits = 512)
  val gemm_queue = new AvalonSourceIO(dataBits = 128)
  val out_mem = Flipped(new AvalonSlaveIO(dataBits = 512, addrBits = 17))
  val uop_mem = Flipped(new AvalonSlaveIO(dataBits = 32, addrBits = 15))
  val acc_mem = Flipped(new AvalonSlaveIO(dataBits = 512, addrBits = 17))
}

class Control(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new ControlSignals)

  val insn            = Reg(UInt())
  when (io.gemm_queue.valid) {
    insn := io.gemm_queue.data
    io.gemm_queue.ready := 1.U
  } .otherwise {
    insn := io.gemm_queue.data
    io.gemm_queue.ready := 0.U
  }

  val opcode          = insn(insn_mem_0_1, insn_mem_0_0)
  val pop_prev_dep    = insn(insn_mem_1)
  val pop_next_dep    = insn(insn_mem_2)
  val push_prev_dep   = insn(insn_mem_3)
  val push_next_dep   = insn(insn_mem_4)

  val uops_data       = Reg(UInt())
  when (io.uops.valid) {
    uops_data := io.uops.data
    io.uops.ready := 1.U
  } .otherwise {
    uops_data := uops_data
    io.uops.ready := 0.U
  }

  val opcode_finish_en = (opcode === opcode_finish.U)
  val opcode_load_en = (opcode === opcode_load.U || opcode === opcode_store.U)
  val opcode_gemm_en = (opcode === opcode_gemm.U || opcode === opcode_alu.U)

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

  val sram_idx = sram_base
  val dram_idx = dram_base
  val y_size_total = y_pad_0 + y_size + y_pad_1
  val x_size_total = y_pad_0 + y_size + y_pad_1
  val y_offset = x_size_total * y_pad_0

  when (insn =/= 0.U) {
    printf(p"=======================================\n")
    printf(p"insn = 0x${Hexadecimal(insn)}\n")
    printf(p"opcode = 0x${Hexadecimal(opcode)}\n")
    printf(p"uops_data = 0x${Hexadecimal(uops_data)}\n")
    printf(p"memory_type = 0x${Hexadecimal(memory_type)}\n")
    printf(p"y_size = 0x${Hexadecimal(y_size)}\n")
    printf(p"x_size = 0x${Hexadecimal(x_size)}\n")
  }

  io.done.waitrequest := 0.U
  io.done.readdata := Mux(opcode_finish_en, 1.U, 0.U)

  // write to uop_mem
  val uop_mem_write_en = (opcode_load_en && (memory_type === mem_id_uop.U) && (!io.uop_mem.waitrequest))
  io.uop_mem.read := 0.U
  io.uop_mem.write := Mux(uop_mem_write_en, 1.U, 0.U)
  io.uop_mem.writedata := uops_data
  io.uop_mem.address := Mux(uop_mem_write_en, insn(insn_mem_6_1, insn_mem_6_0), 0.U)

  // write to acc_mem
  val acc_cntr_en = io.biases.valid && (!io.acc_mem.waitrequest)
  val (acc_x_cntr_val, acc_x_cntr_wrap) = Counter(acc_cntr_en, 8)
  io.biases.ready := acc_cntr_en
  io.acc_mem.address := ((sram_idx + y_offset + (acc_y_cntr_val * x_pad_0)) * batch.U + acc_x_cntr_val)
  io.acc_mem.read := 0.U
  io.acc_mem.write := acc_cntr_en
  io.acc_mem.writedata := io.biases.data

  // write to out_mem
  io.out_mem.address := 0.U
  io.out_mem.read := 0.U
  io.out_mem.write := 0.U
  io.out_mem.writedata := 0.U
}
