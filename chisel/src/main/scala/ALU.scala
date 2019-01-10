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

class ALUIO(implicit p: Parameters) extends CoreBundle()(p) {
  val A = Input(UInt((block_out * acc_width).W))
  val B = Input(UInt((block_out * acc_width).W))
  val alu_op = Input(UInt(alu_opcode_bit_width.W))
  val out = Output(UInt((block_out * acc_width).W))
  val out_short = Output(UInt((block_out * out_width).W))
}

import ALU._

class ALU(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new ALUIO())
  val shamt = io.B(4,0).asUInt
  io.out := MuxLookup(io.alu_op, 0.U, Seq(
    ALU_MIN -> Mux(io.A < io.B, io.A, io.B),
    ALU_MAX -> Mux(io.A < io.B, io.B, io.A),
    ALU_ADD -> (io.A + io.B),
    ALU_SHR -> (io.A >> shamt)
  ))
}
