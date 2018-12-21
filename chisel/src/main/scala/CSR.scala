// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.{Parameters, Field}

class CSRIO(implicit p: Parameters) extends CoreBundle()(p) {
}

class CSR(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new CSRIO)
}
