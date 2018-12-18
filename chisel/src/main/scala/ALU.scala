// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters
  
object ALU {
  val ALU_MIN    = 0.U(4.W)
  val ALU_MAX    = 1.U(4.W)
  val ALU_ADD    = 2.U(4.W)
  val ALU_SHR    = 3.U(4.W)
  val ALU_XXX    = 15.U(4.W)
}

import ALU._

// abstract class ALU(implicit val p: Parameters) extends Module {
// }

class ALUSimple extends Module {
  val io = IO(new Bundle {
    val A      = Input(UInt(8.W))
    val B      = Input(UInt(8.W))
    val opcode = Input(UInt(4.W))
    val out    = Output(UInt(8.W))
  })

  val shamt = io.B(4,0).asUInt
  io.out := MuxLookup(io.opcode, io.B, Seq(
    ALU_MIN -> Mux(io.A < io.B, io.A, io.B),
    ALU_MAX -> Mux(io.A < io.B, io.B, io.A),
    ALU_ADD -> (io.A + io.B),
    ALU_SHR -> (io.A >> 1)
  ))
}
