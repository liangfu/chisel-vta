// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.{Parameters, Field}

class DatapathIO(implicit p: Parameters) extends CoreBundle()(p) {
}

class Datapath(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new DatapathIO)

  val csr = Module(new CSR)

  val started = RegNext(reset.toBool)

}
