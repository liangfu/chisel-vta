// See LICENSE for license details.

package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters

class ControlSignals(implicit p: Parameters) extends CoreBundle()(p) {
  val done = new AvalonSlaveIO(dataBits = 1, addrBits = 1)
  val uops = new AvalonSourceIO(dataBits = 32)
  val gemm_queue = new AvalonSourceIO(dataBits = 128)
  val uop_mem = new AvalonMasterIO(dataBits = 64, addrBits = 15)
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

  io.done.waitrequest := 0.U
  io.uop_mem.address := 0.U
  io.uop_mem.read := 0.U
  io.uop_mem.write := 1.U
  io.uop_mem.writedata := io.uops.data

  when (opcode === opcode_finish.U){
    io.done.readdata := 1.U
  } .elsewhen (opcode === opcode_load.U || opcode === opcode_store.U) {
    io.done.readdata := 0.U

    val memory_type = insn(insn_mem_5_1, insn_mem_5_0)
    // val sram_base   = insn(insn_mem_6_1, insn_mem_6_0)
    // val dram_base   = insn(insn_mem_7_1, insn_mem_7_0)
    // val y_size      = insn(insn_mem_8_1, insn_mem_8_0)
    // val x_size      = insn(insn_mem_9_1, insn_mem_9_0)
    // val x_stride    = insn(insn_mem_a_1, insn_mem_a_0)
    // val y_pad_0     = insn(insn_mem_b_1, insn_mem_b_0)
    // val y_pad_1     = insn(insn_mem_c_1, insn_mem_c_0)
    // val x_pad_0     = insn(insn_mem_d_1, insn_mem_d_0)
    // val x_pad_1     = insn(insn_mem_e_1, insn_mem_e_0) 

    when (memory_type === mem_id_uop.U) {
      // uop_mem[sram_base] = uops.read();
      when (io.uop_mem.waitrequest) {
        // TODO
      }
    }

  } .elsewhen (opcode === opcode_gemm.U || opcode === opcode_alu.U) {
    io.done.readdata := 0.U
  } .otherwise {
    io.done.readdata := 0.U
  }
}
