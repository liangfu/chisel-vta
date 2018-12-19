// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.{Parameters, Field}

class AvalonSlaveIO(val dataBits: Int = 32, val addrBits: Int = 1)(implicit p: Parameters) extends CoreBundle()(p) {
  val waitrequest = Output(UInt(1.W))
  val address = Input(UInt(addrBits.W))
  val read = Input(UInt(1.W))
  val readdata = Output(UInt(dataBits.W))
  val wite = Input(UInt(1.W))
  val witedata = Input(UInt(dataBits.W))
}
  
class AvalonSourceIO(val dataBits: Int = 32)(implicit p: Parameters) extends CoreBundle()(p) {
  val ready = Output(UInt(1.W))
  val valid = Input(UInt(1.W))
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
  val out_mem = Flipped(new AvalonSlaveIO(dataBits = 64, addrBits = 17))
  val uop_mem = Flipped(new AvalonSlaveIO(dataBits = 64, addrBits = 15))
  val acc_mem = Flipped(new AvalonSlaveIO(dataBits = 64, addrBits = 17))
}

abstract class Compute(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new ComputeIO())
}

class ComputeSimple(implicit p: Parameters) extends Compute()(p) {
  val core = Module(new Core)

  io.done <> DontCare
  io.uops <> DontCare
  io.biases <> DontCare
  io.gemm_queue <> core.io.gemm_queue
  io.l2g_dep_queue <> DontCare
  io.s2g_dep_queue <> DontCare
  io.g2l_dep_queue <> DontCare
  io.g2s_dep_queue <> DontCare
  io.inp_mem <> DontCare
  io.wgt_mem <> DontCare
  io.out_mem <> DontCare
  io.uop_mem <> DontCare
  io.acc_mem <> DontCare
}
