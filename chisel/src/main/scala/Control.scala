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
  val opcode_gemm_en = (opcode === opcode_gemm.U)
  val opcode_alu_en = (opcode === opcode_alu.U)

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
  val acc_cntr_en = (opcode_load_en && (memory_type === mem_id_acc.U) &&
                     io.biases.valid && (!io.acc_mem.waitrequest))
  val (acc_x_cntr_val, acc_x_cntr_wrap) = Counter(acc_cntr_en, 8)
  io.biases.ready := acc_cntr_en
  io.acc_mem.address := ((sram_idx + y_offset) * batch.U + acc_x_cntr_val)
  io.acc_mem.read := 0.U
  io.acc_mem.write := acc_cntr_en
  io.acc_mem.writedata := io.biases.data
  when ((insn =/= 0.U) && acc_cntr_en) {
    printf(p"y_size = 0x${Hexadecimal(y_size)}\n")
    printf(p"x_size = 0x${Hexadecimal(x_size)}\n")
    printf(p"acc_x_cntr_val = 0x${Hexadecimal(acc_x_cntr_val)}\n")
    printf(p"acc_x_cntr_wrap = 0x${Hexadecimal(acc_x_cntr_wrap)}\n")
    printf(p"io.biases.data = 0x${Hexadecimal(io.biases.data)}\n")
  }

  // write to out_mem
  val uop_bgn = insn(insn_gem_6_1, insn_gem_6_0)
  val uop_end = insn(insn_gem_7_1, insn_gem_7_0)
  val iter_out = insn(insn_gem_8_1, insn_gem_8_0)
  val iter_in = insn(insn_gem_9_1, insn_gem_9_0)
  val alu_opcode = insn(insn_alu_e_1, insn_alu_e_0)
  val use_imm = insn(insn_alu_f)
  val imm = insn(insn_alu_g_1, insn_alu_g_0)
  val in_loop_cntr_en = opcode_alu_en
  val (in_loop_cntr_val, in_loop_cntr_wrap) = Counter(in_loop_cntr_en, 8)

  val upc = 0.U
  io.uop_mem.address := upc
  io.uop_mem.read := 1.U
  val uop = RegInit(io.uop_mem.readdata)
  val dst_idx = uop(uop_alu_0_1, uop_alu_0_0)
  val src_idx = uop(uop_alu_1_1, uop_alu_1_0)

  val acc_mem_read_cntr_en = opcode_alu_en
  val (acc_mem_read_cntr_val, acc_mem_read_cntr_wrap) = Counter(acc_mem_read_cntr_en, 2)
  io.acc_mem.address := Mux(acc_mem_read_cntr_val === 0.U, dst_idx, src_idx)
  val dst_vector = io.acc_mem.readdata
  val src_vector = io.acc_mem.readdata
  // val cmp_res = Reg(UInt())
  // val short_cmp_res = Reg(UInt())

  val alu_block_cntr_en = opcode_alu_en
  val (alu_block_cntr_val, alu_block_cntr_wrap) = Counter(alu_block_cntr_en, 16)
  val b = alu_block_cntr_val

  // val src_0 = dst_vector((b + 1) * acc_width - 1, b * acc_width)
  // val src_1 = Mux(use_imm, imm, src_vector((b + 1) * acc_width - 1, b * acc_width))
  // val mix_val = Mux(src_0 < src_1, Mux(alu_opcode === alu_opcode_min, src_0, src_1),
  //                                  Mux(alu_opcode === alu_opcode_min, src_1, src_0))
  // cmp_res((b + 1) * acc_width_width - 1, b * acc_width) := mix_val

  io.out_mem.address := 0.U
  io.out_mem.read := 0.U
  io.out_mem.write := 0.U
  io.out_mem.writedata := 0.U
  when ((insn =/= 0.U) && in_loop_cntr_en) {
    printf(p"iter_out = 0x${Hexadecimal(iter_out)}\n")
    printf(p"iter_in = 0x${Hexadecimal(iter_in)}\n")
    printf(p"uop_bgn = 0x${Hexadecimal(uop_bgn)}\n")
    printf(p"uop_end = 0x${Hexadecimal(uop_end)}\n")
    printf(p"batch = 0x${Hexadecimal(batch.U)}\n")
    printf(p"block_out = 0x${Hexadecimal(block_out.U)}\n")
    printf(p"in_loop_cntr_val = 0x${Hexadecimal(in_loop_cntr_val)}\n")
    printf(p"in_loop_cntr_wrap = 0x${Hexadecimal(in_loop_cntr_wrap)}\n")
    printf(p"uop = 0x${Hexadecimal(uop)}\n")
    printf(p"dst_idx = 0x${Hexadecimal(dst_idx)}\n")
    printf(p"src_idx = 0x${Hexadecimal(src_idx)}\n")
    printf(p"dst_vector = 0x${Hexadecimal(dst_vector)}\n")
    printf(p"src_vector = 0x${Hexadecimal(src_vector)}\n")
  }
  when (insn =/= 0.U) {
    printf(p"=======================================\n")
  }
}
