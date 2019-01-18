// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.{Parameters, Field}

class StoreIO(implicit p: Parameters) extends CoreBundle()(p) {
  val outputs = Flipped(new AvalonSlaveIO(dataBits = 128, addrBits = 32))
  val store_queue = new AvalonSinkIO(dataBits = 128)
  val s2g_dep_queue = Flipped(new AvalonSinkIO(dataBits = 1))
  val g2s_dep_queue = new AvalonSinkIO(dataBits = 1)
  val out_mem = Flipped(new AvalonSlaveIO(dataBits = 128, addrBits = 17))
}

class Store(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new StoreIO())

  val insn            = Reg(UInt(128.W))
  val insn_valid      = insn =/= 0.U

  val g2s_dep_queue_valid = Reg(Bool())
  val g2s_dep_queue_wait = Reg(Bool())
  val s2g_dep_queue_done = Reg(Bool())

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

  // fifo buffer
  val out_queue = Module(new Queue(UInt(128.W), 8))

  // counters
  val enq_cntr_max = x_size * y_size
  val enq_cntr_wait = !out_queue.io.enq.ready || io.out_mem.waitrequest
  val enq_cntr_val = Reg(UInt(32.W))
  val enq_cntr_wrap = RegInit(0.U)
  val enq_cntr_en = enq_cntr_val < enq_cntr_max

  val deq_cntr_max = x_size * y_size
  val deq_cntr_en = out_queue.io.deq.valid
  val deq_cntr_wait = io.outputs.waitrequest
  val deq_cntr_val = Reg(UInt(32.W))
  val deq_cntr_wrap = RegInit(0.U)

  // status registers
  // val state = Reg(UInt(8.W))
  val busy = Mux(pop_prev_dependence && (!g2s_dep_queue_valid && g2s_dep_queue_wait), 1.U,
             Mux(push_prev_dependence && !s2g_dep_queue_done, 1.U,
             Mux((enq_cntr_en && !enq_cntr_wait) && (enq_cntr_val < enq_cntr_max), 1.U,
             Mux((deq_cntr_en && !deq_cntr_wait) && (deq_cntr_val < deq_cntr_max), 1.U, 0.U))))

  // setup counter
  when (enq_cntr_en && !enq_cntr_wait) {
    when (enq_cntr_en) {
      enq_cntr_val := enq_cntr_val + 1.U
    } .otherwise {
      enq_cntr_val := enq_cntr_val
      enq_cntr_wrap := 1.U
    }
  } .elsewhen (!busy) {
    enq_cntr_val := 0.U
    enq_cntr_wrap := 0.U
  } .otherwise {
    enq_cntr_val := enq_cntr_val
  }

  when (deq_cntr_en && !deq_cntr_wait) {
    when (deq_cntr_en) {
      deq_cntr_val := deq_cntr_val + 1.U
    } .otherwise {
      deq_cntr_val := deq_cntr_val
      deq_cntr_wrap := 1.U
    }
  } .elsewhen (!busy) {
    deq_cntr_val := 0.U
    deq_cntr_wrap := 0.U
  } .otherwise {
    deq_cntr_val := deq_cntr_val
  }
  
  // fetch instruction
  when (io.store_queue.valid && !busy) {
    insn := io.store_queue.data
    io.store_queue.ready := 1.U
  } .otherwise {
    insn := insn
    io.store_queue.ready := 0.U
  }

  // dequeue g2s_dep_queue
  when (pop_prev_dependence && io.g2s_dep_queue.valid) {
    io.g2s_dep_queue.ready := 1.U
    g2s_dep_queue_valid := 1.U
  } .otherwise {
    io.g2s_dep_queue.ready := 0.U
  }

  // enqueue from out_mem to fifo
  io.out_mem.address := (sram_idx * batch.U + enq_cntr_val) << 4.U
  io.out_mem.write := RegNext(0.U)
  io.out_mem.writedata <> DontCare
  io.out_mem.read := enq_cntr_en
  out_queue.io.enq.valid := enq_cntr_en && !enq_cntr_wait
  out_queue.io.enq.bits := io.out_mem.readdata

  // dequeue fifo and send to outputs
  out_queue.io.deq.ready := deq_cntr_en && !deq_cntr_wait
  io.outputs.write := deq_cntr_en
  io.outputs.writedata := out_queue.io.deq.bits
  io.outputs.address := (dram_idx * batch.U + deq_cntr_val) << 4.U
  io.outputs.read := RegNext(0.U)

  // enqueue s2g_dep_queue
  io.s2g_dep_queue.data := RegNext(1.U)
  when (push_prev_dependence && !s2g_dep_queue_done) {
    io.s2g_dep_queue.valid := RegNext(1.U)
    s2g_dep_queue_done := 1.U
  } .otherwise {
    io.s2g_dep_queue.valid := 0.U
  }

}

