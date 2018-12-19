package vta

import chisel3._
import freechips.rocketchip.config.{Parameters, Field}

case object XLEN extends Field[Int]
case object LOG_INP_WIDTH extends Field[Int]
case object LOG_WGT_WIDTH extends Field[Int]
case object LOG_ACC_WIDTH extends Field[Int]
case object LOG_OUT_WIDTH extends Field[Int]
case object LOG_BATCH extends Field[Int]
case object LOG_BLOCK_IN extends Field[Int]
case object LOG_BLOCK_OUT extends Field[Int]
case object BuildALU extends Field[Parameters => ALU]

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
}

abstract class CoreBundle(implicit val p: Parameters) extends Bundle with CoreParams

class CoreIO(implicit p: Parameters) extends CoreBundle()(p) {
  val done = new AvalonSlaveIO
}

class Core(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new CoreIO)
  // val dpath = Module(new Datapath) 
  // val ctrl  = Module(new Control)

  // io.done <> dpath.io.done
  // dpath.io.icache <> io.icache
  // dpath.io.dcache <> io.dcache
  // dpath.io.ctrl <> ctrl.io
}

