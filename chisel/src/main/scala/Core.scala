package vta

import chisel3._
import freechips.rocketchip.config.{Parameters, Field}

case object XLEN extends Field[Int]
case object BuildALU extends Field[Parameters => ALU]

abstract trait CoreParams {
  implicit val p: Parameters
  val xlen = p(XLEN)
}

abstract class CoreBundle(implicit val p: Parameters) extends Bundle with CoreParams
