// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.{Parameters, Field}

class DatapathIO(implicit p: Parameters) extends CoreBundle()(p) {
  val gemm_queue = new AvalonSourceIO(dataBits = 128)
  val insn = Output(UInt(128.W))
}

class Datapath(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new DatapathIO)
  io.insn := Mux(io.gemm_queue.valid === 1.U, io.gemm_queue.data, 0.U(128.W))
  io.gemm_queue.ready := Mux(io.gemm_queue.valid === 1.U, 1.U, 0.U)
}
