package vta

import chisel3._
import freechips.rocketchip.config.{Parameters, Field}

case object XLEN extends Field[Int]
case object Trace extends Field[Boolean]

case object LOG_INP_WIDTH extends Field[Int]
case object LOG_WGT_WIDTH extends Field[Int]
case object LOG_ACC_WIDTH extends Field[Int]
case object LOG_OUT_WIDTH extends Field[Int]
case object LOG_BATCH extends Field[Int]
case object LOG_BLOCK_IN extends Field[Int]
case object LOG_BLOCK_OUT extends Field[Int]
case object BuildALU extends Field[Parameters => ALU]

case object OPCODE_BIT_WIDTH extends Field[Int]

case object OPCODE_LOAD extends Field[Int]
case object OPCODE_STORE extends Field[Int]
case object OPCODE_GEMM extends Field[Int]
case object OPCODE_FINISH extends Field[Int]
case object OPCODE_ALU extends Field[Int]

case object ALU_OPCODE_MIN extends Field[Int]
case object ALU_OPCODE_MAX extends Field[Int]
case object ALU_OPCODE_ADD extends Field[Int]
case object ALU_OPCODE_SHR extends Field[Int]


abstract trait CoreParams {
  implicit val p: Parameters
  val xlen = p(XLEN)
  val log_inp_width = p(LOG_INP_WIDTH)
  val log_wgt_width = p(LOG_WGT_WIDTH)
  val log_acc_width = p(LOG_ACC_WIDTH)
  val log_out_width = p(LOG_OUT_WIDTH)
  val inp_width = 1 << p(LOG_INP_WIDTH)
  val wgt_width = 1 << p(LOG_WGT_WIDTH)
  val acc_width = 1 << p(LOG_ACC_WIDTH)
  val out_width = 1 << p(LOG_OUT_WIDTH)

  val opcode_bit_width = 3
  val alu_opcode_bit_width = 2
  val memop_id_bit_width = 2
  val memop_sram_addr_bit_width = 16
  val memop_dram_addr_bit_width = 32
  val memop_size_bit_width = 16
  val memop_stride_bit_width = 16
  val memop_pad_bit_width = 4
  val memop_pad_val_bit_width = 2
  val aluop_imm_bit_width = 16

  val mem_id_uop = 0
  val mem_id_wgt = 1
  val mem_id_inp = 2
  val mem_id_acc = 3
  val mem_id_out = 4

  val opcode_load = p(OPCODE_LOAD)
  val opcode_store = p(OPCODE_STORE)
  val opcode_gemm = p(OPCODE_GEMM)
  val opcode_finish = p(OPCODE_FINISH)
  val opcode_alu = p(OPCODE_ALU)

  val alu_opcode_min = p(ALU_OPCODE_MIN)
  val alu_opcode_max = p(ALU_OPCODE_MAX)
  val alu_opcode_add = p(ALU_OPCODE_ADD)
  val alu_opcode_shr = p(ALU_OPCODE_SHR)

  val insn_mem_0_0 = 0
  val insn_mem_0_1 = opcode_bit_width - 1
  val insn_mem_1 = insn_mem_0_1 + 1
  val insn_mem_2 = insn_mem_1 + 1
  val insn_mem_3 = insn_mem_2 + 1
  val insn_mem_4 = insn_mem_3 + 1
  val insn_mem_5_0 = insn_mem_4 + 1
  val insn_mem_5_1 = insn_mem_5_0 + memop_id_bit_width - 1
  val insn_mem_6_0 = insn_mem_5_1 + 1
  val insn_mem_6_1 = insn_mem_6_0 + memop_sram_addr_bit_width - 1
  val insn_mem_7_0 = insn_mem_6_1 + 1
  val insn_mem_7_1 = insn_mem_7_0 + memop_dram_addr_bit_width - 1
  val insn_mem_8_0 = insn_mem_7_1 + 1
  val insn_mem_8_1 = insn_mem_8_0 + memop_size_bit_width - 1
  val insn_mem_9_0 = insn_mem_8_1 + 1
  val insn_mem_9_1 = insn_mem_9_0 + memop_size_bit_width - 1
  val insn_mem_a_0 = insn_mem_9_1 + 1
  val insn_mem_a_1 = insn_mem_a_0 + memop_stride_bit_width - 1
  val insn_mem_b_0 = insn_mem_a_1 + 1
  val insn_mem_b_1 = insn_mem_b_0 + memop_pad_bit_width - 1
  val insn_mem_c_0 = insn_mem_b_1 + 1
  val insn_mem_c_1 = insn_mem_c_0 + memop_pad_bit_width - 1
  val insn_mem_d_0 = insn_mem_c_1 + 1
  val insn_mem_d_1 = insn_mem_d_0 + memop_pad_bit_width - 1
  val insn_mem_e_0 = insn_mem_d_1 + 1
  val insn_mem_e_1 = insn_mem_e_0 + memop_pad_bit_width - 1
}

abstract class CoreBundle(implicit val p: Parameters) extends Bundle with CoreParams

class CoreIO(implicit p: Parameters) extends CoreBundle()(p) {
  val done = new AvalonSlaveIO(dataBits = 1, addrBits = 1)
  val uops = new AvalonSourceIO(dataBits = 32)
  val gemm_queue = new AvalonSourceIO(dataBits = 128)
  val uop_mem = Flipped(new AvalonSlaveIO(dataBits = 32, addrBits = 15))
}

class Core(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new CoreIO)
  // val dpath = Module(new Datapath) 
  val ctrl  = Module(new Control)

  // dpath.io.gemm_queue <> io.gemm_queue
  ctrl.io.done <> io.done
  ctrl.io.uops <> io.uops
  ctrl.io.gemm_queue <> io.gemm_queue
  ctrl.io.uop_mem <> io.uop_mem
  // dpath.io.icache <> io.icache
  // dpath.io.dcache <> io.dcache
  // dpath.io.ctrl <> ctrl.io
}

