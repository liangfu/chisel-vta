// See LICENSE for license details.

package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters

object Control {
  val Y = true.B
  val N = false.B

  import ALU._

  // def Slice = (d: UInt, x: UInt, w: Int) => ((d >> (x)) & ((1.U << w.U) - 1.U))(w - 1, 0)
  def Slice = (d: UInt, x: UInt, w: Int) => (d >> (x))(w - 1, 0)
}

class ControlSignals(implicit p: Parameters) extends CoreBundle()(p) {
  val done = new AvalonSlaveIO(dataBits = 1, addrBits = 1)
  val uops = new AvalonSourceIO(dataBits = 32)
  val biases = new AvalonSourceIO(dataBits = 512)
  val gemm_queue = new AvalonSourceIO(dataBits = 128)
  val out_mem = Flipped(new AvalonSlaveIO(dataBits = 512, addrBits = 17))
  val uop_mem = Flipped(new AvalonSlaveIO(dataBits = 32, addrBits = 15))
  // val acc_mem = Flipped(new AvalonSlaveIO(dataBits = 512, addrBits = 17))
}

class Control(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new ControlSignals)

  val acc_mem = Mem(1 << 17, UInt(512.W))

  import Control._

  val insn            = Reg(UInt(128.W))

  val opcode          = insn(insn_mem_0_1, insn_mem_0_0)
  val pop_prev_dep    = insn(insn_mem_1)
  val pop_next_dep    = insn(insn_mem_2)
  val push_prev_dep   = insn(insn_mem_3)
  val push_next_dep   = insn(insn_mem_4)

  val uops_data       = Reg(UInt(32.W))

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

  // control and status registers (csr)
  val opcode_finish_en = (opcode === opcode_finish.U)
  val opcode_load_en = (opcode === opcode_load.U || opcode === opcode_store.U)
  val opcode_gemm_en = (opcode === opcode_gemm.U)
  val opcode_alu_en = (opcode === opcode_alu.U)

  val memory_type_uop_en = memory_type === mem_id_uop.U
  val memory_type_acc_en = memory_type === mem_id_acc.U

  // counters
  val acc_cntr_en = (opcode_load_en && memory_type_acc_en && io.biases.valid)
  val (acc_x_cntr_val, acc_x_cntr_wrap) = Counter(acc_cntr_en, 8)
  val in_loop_cntr_en = opcode_alu_en || opcode_gemm_en
  val (in_loop_cntr_val, in_loop_cntr_wrap) = Counter(in_loop_cntr_en, 8)

  // status
  val started = 1.U(1.W)
  val busy = Mux(opcode_load_en && memory_type_uop_en, 0.U,
             Mux(acc_cntr_en && (acc_x_cntr_wrap =/= 1.U), 1.U, 
             Mux(in_loop_cntr_en && (in_loop_cntr_wrap =/= 1.U), 1.U, 0.U)))
  val done = 1.U(1.W)

  // fetch instruction
  when (io.gemm_queue.valid && (busy === 0.U)) {
    insn := io.gemm_queue.data
    io.gemm_queue.ready := 1.U
  } .otherwise {
    insn := io.gemm_queue.data
    io.gemm_queue.ready := 0.U
  }

  // fetch uops
  when (io.uops.valid) {
    uops_data := io.uops.data
    io.uops.ready := 1.U
  } .otherwise {
    uops_data := uops_data
    io.uops.ready := 0.U
  }

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
  val acc_mem_addr = ((sram_idx + y_offset + x_pad_0) * batch.U + acc_x_cntr_val)
  when (acc_cntr_en) {
    acc_mem(acc_mem_addr) := io.biases.data
  }
  io.biases.ready := acc_cntr_en
  when ((insn =/= 0.U) && acc_cntr_en) {
    printf(p"x_pad_0 = 0x${Hexadecimal(x_pad_0)}\n")
    printf(p"x_pad_1 = 0x${Hexadecimal(x_pad_1)}\n")
    printf(p"y_size = 0x${Hexadecimal(y_size)}\n")
    printf(p"x_size = 0x${Hexadecimal(x_size)}\n")
    printf(p"acc_cntr_en = 0x${Hexadecimal(acc_cntr_en)}\n")
    printf(p"acc_x_cntr_val = 0x${Hexadecimal(acc_x_cntr_val)}\n")
    printf(p"acc_x_cntr_wrap = 0x${Hexadecimal(acc_x_cntr_wrap)}\n")
    printf(p"acc_mem_addr = 0x${Hexadecimal(acc_mem_addr)}\n")
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
  val it_in = in_loop_cntr_val

  val upc = 0.U
  io.uop_mem.address := upc
  io.uop_mem.read := 1.U
  val uop = io.uop_mem.readdata
  val dst_offset_in = it_in
  val src_offset_in = it_in
  val dst_idx = uop(uop_alu_0_1, uop_alu_0_0) + dst_offset_in
  val src_idx = uop(uop_alu_1_1, uop_alu_1_0) + src_offset_in

  val dst_vector = acc_mem(dst_idx)
  val src_vector = acc_mem(src_idx)
  val cmp_res = Wire(Vec(block_out, UInt(acc_width.W)))
  val short_cmp_res = Wire(Vec(block_out, UInt(out_width.W)))
  val add_res = Wire(Vec(block_out, UInt(acc_width.W)))
  val short_add_res = Wire(Vec(block_out, UInt(out_width.W)))
  val shr_res = Wire(Vec(block_out, UInt(acc_width.W)))
  val short_shr_res = Wire(Vec(block_out, UInt(out_width.W)))

  for (i <- 0 to (block_out - 1)) {
    cmp_res(i) := 0.U
    short_cmp_res(i) := 0.U
    add_res(i) := 0.U
    short_add_res(i) := 0.U
    shr_res(i) := 0.U
    short_shr_res(i) := 0.U
  }

  // loop unroll
  when ((insn =/= 0.U) && in_loop_cntr_en) {
  for (b <- 0 to (block_out - 1)) {
    val src_0 = Slice(dst_vector, (b * acc_width).U, acc_width)
    val src_1 = Mux(use_imm, imm, Slice(src_vector, (b * acc_width).U, acc_width))
    val mix_val = Mux(src_0.asSInt < src_1.asSInt, Mux(alu_opcode === alu_opcode_min.U, src_0, src_1),
                                                   Mux(alu_opcode === alu_opcode_min.U, src_1, src_0))
    cmp_res(b) := mix_val
    short_cmp_res(b) := Slice(mix_val.asUInt, 0.U, out_width)
    val add_val = Slice(src_0, 0.U, acc_width).asSInt + Slice(src_1, 0.U, acc_width).asSInt
    add_res(b) := add_val.toUInt
    add_res(b) := Slice(add_val.asUInt, 0.U, out_width)
    val shr_val = Slice(src_0, 0.U, acc_width) >> Slice(src_1, 0.U, log_acc_width)
    shr_res(b) := shr_val
    shr_res(b) := Slice(shr_val.asUInt, 0.U, out_width)
    // printf(p"+---src_0 = 0x${Hexadecimal(src_0)}\n")
    // printf(p"|   src_1 = 0x${Hexadecimal(src_1)}\n")
    // printf(p"|   mix_val = 0x${Hexadecimal(mix_val.asUInt)}\n")
  }
  }

  io.out_mem.address := dst_idx
  io.out_mem.read := 0.U
  io.out_mem.write := opcode_alu_en
  val alu_opcode_minmax_en = (alu_opcode === alu_opcode_min.U) || (alu_opcode === alu_opcode_max.U)
  val alu_opcode_add_en = (alu_opcode === alu_opcode_add.U)
  io.out_mem.writedata := Mux(alu_opcode_minmax_en, Cat(short_cmp_res.init.reverse),
                          Mux(alu_opcode_add_en, Cat(short_add_res.init.reverse), Cat(short_shr_res.init.reverse)))

  // when (in_loop_cntr_en) {
  //   acc_mem(dst_idx) := cmp_res
  // }

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
    printf(p"use_imm = 0x${Hexadecimal(use_imm)}\n")
    printf(p"imm = 0x${Hexadecimal(imm)}\n")
    printf(p"acc_width = 0x${Hexadecimal(acc_width.U)}\n")
    printf(p"out_width = 0x${Hexadecimal(out_width.U)}\n")
    printf(p"cmp_res = 0x${Hexadecimal(Cat(cmp_res.init))}\n")
    printf(p"short_cmp_res = 0x${Hexadecimal(Cat(short_cmp_res.init))}\n")
  }
  when (insn =/= 0.U) {
    printf(p"=======================================\n")
  }
}
