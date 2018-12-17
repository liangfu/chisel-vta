// See LICENSE for license details.

package mini

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters

object ALU {
  val ALU_MIN    = 0.U(4.W)
  val ALU_MAX    = 1.U(4.W)
  val ALU_ADD    = 2.U(4.W)
  val ALU_SHR    = 3.U(4.W)
}

class ALUIo(implicit p: Parameters) extends CoreBundle()(p) {
  val A = Input(UInt(xlen.W))
  val B = Input(UInt(xlen.W))
  val alu_op = Input(UInt(4.W))
  val out = Output(UInt(xlen.W))
  val sum = Output(UInt(xlen.W))
}

import ALU._

abstract class ALU(implicit val p: Parameters) extends Module with CoreParams {
  val io = IO(new ALUIo)
}

class ALUSimple(implicit p: Parameters) extends ALU()(p) {
  val shamt = io.B(4,0).asUInt

  io.out := MuxLookup(io.alu_op, io.B, Seq(
      ALU_MIN  -> Mux(io.A < io.B, io.A, io.B),
      ALU_MAX  -> Mux(io.A < io.B, io.B, io.A),
      ALU_ADD  -> (io.A + io.B),
      ALU_SHR  -> (io.A >> shamt)
      ))

  io.sum := io.A + Mux(io.alu_op(0), -io.B, io.B)
}

class ALUArea(implicit p: Parameters) extends ALU()(p) { 
  val sum = io.A + Mux(io.alu_op(0), -io.B, io.B)
  val cmp = Mux(io.A < io.B, io.A, io.B)
  val shamt  = io.B(4,0).asUInt
  val shin   = Mux(io.alu_op(3), io.A, Reverse(io.A))
  val shiftr = (Cat(io.alu_op(0) && shin(xlen-1), shin).asSInt >> shamt)(xlen-1, 0)
  val shiftl = Reverse(shiftr)

  val out = 
    Mux(io.alu_op === ALU_MIN || io.alu_op === ALU_MAX, cmp,
    Mux(io.alu_op === ALU_ADD, sum,
    Mux(io.alu_op === ALU_SHR, shiftr)))


  io.out := out
  io.sum := sum
}
