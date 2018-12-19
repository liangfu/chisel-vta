// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters
  
class AvalonSlaveIO(implicit p: Parameters) extends CoreBundle()(p) {
  val waitrequest = Output(UInt(1.W))
  val address = Input(UInt(12.W))
  val read = Input(UInt(1.W))
  val readdata = Input(UInt(32.W))
  val wite = Input(UInt(1.W))
  val witedata = Output(UInt(32.W))
}
  
class AvalonSourceIO(implicit p: Parameters) extends CoreBundle()(p) {
  val ready = Output(UInt(1.W))
  val valid = Input(UInt(1.W))
  val data = Input(UInt(32.W))
}

class ComputeIO(implicit p: Parameters) extends CoreBundle()(p) {
  val done = new AvalonSlaveIO
  val uops = Flipped(new AvalonSourceIO)
  val biases = Flipped(new AvalonSourceIO)
  val gemm_queue = Flipped(new AvalonSourceIO)
  val l2g_dep_queue = Flipped(new AvalonSourceIO)
  val s2g_dep_queue = Flipped(new AvalonSourceIO)
  val g2l_dep_queue = new AvalonSourceIO
  val g2s_dep_queue = new AvalonSourceIO
  val inp_mem = Flipped(new AvalonSlaveIO)
  val wgt_mem = Flipped(new AvalonSlaveIO)
  val out_mem = Flipped(new AvalonSlaveIO)
  val uop_mem = Flipped(new AvalonSlaveIO)
  val acc_mem = Flipped(new AvalonSlaveIO)
}

abstract class Compute(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new ComputeIO())
}

class ComputeSimple(implicit p: Parameters) extends Compute()(p) {
  io.done <> DontCare
  io.uops <> DontCare
  io.biases <> DontCare
  io.gemm_queue <> DontCare
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
