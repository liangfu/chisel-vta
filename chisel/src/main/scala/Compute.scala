// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.{Parameters, Field}

class AvalonSlaveIO(val dataBits: Int = 32, val addrBits: Int = 1)(implicit p: Parameters) extends CoreBundle()(p) {
  val waitrequest = Output(Bool())
  val address = Input(UInt(addrBits.W))
  val read = Input(Bool())
  val readdata = Output(UInt(dataBits.W))
  val write = Input(Bool())
  val writedata = Input(UInt(dataBits.W))
}
  
class AvalonSourceIO(val dataBits: Int = 32)(implicit p: Parameters) extends CoreBundle()(p) {
  val ready = Output(Bool())
  val valid = Input(Bool())
  val data = Input(UInt(dataBits.W))
}

class ComputeIO(implicit p: Parameters) extends CoreBundle()(p) {
  val done = new AvalonSlaveIO(dataBits = 1, addrBits = 1)
  val uops = new AvalonSourceIO(dataBits = 32)
  val biases = new AvalonSourceIO(dataBits = 512)
  val gemm_queue = new AvalonSourceIO(dataBits = 128)
  val l2g_dep_queue = new AvalonSourceIO(dataBits = 1)
  val s2g_dep_queue = new AvalonSourceIO(dataBits = 1)
  val g2l_dep_queue = Flipped(new AvalonSourceIO(dataBits = 1))
  val g2s_dep_queue = Flipped(new AvalonSourceIO(dataBits = 1))
  val inp_mem = Flipped(new AvalonSlaveIO(dataBits = 64, addrBits = 15))
  val wgt_mem = Flipped(new AvalonSlaveIO(dataBits = 64, addrBits = 18))
  val out_mem = Flipped(new AvalonSlaveIO(dataBits = 512, addrBits = 17))
}

class Compute(implicit val p: Parameters) extends Module with CoreParams {
  // implicit val p = params
  val io = IO(new ComputeIO())
  val core = Module(new Core)

  io.done <> core.io.done
  io.uops <> core.io.uops
  io.biases <> core.io.biases
  io.gemm_queue <> core.io.gemm_queue
  io.l2g_dep_queue <> DontCare
  io.s2g_dep_queue <> DontCare
  io.g2l_dep_queue <> DontCare
  io.g2s_dep_queue <> DontCare
  io.inp_mem <> DontCare
  io.wgt_mem <> DontCare
  io.out_mem <> core.io.out_mem
  // io.uop_mem <> core.io.uop_mem
  // io.acc_mem <> DontCare
}

