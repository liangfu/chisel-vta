// See LICENSE.txt for license details.
package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters

// object MemBlock {
//   def apply(addrBits : Int, dataBits : Int, bypass : Boolean = true) = {
//     Module(new MemBlock(addrBits, dataBits, bypass))
//   }
// }

class MemBlockIO(val addrBits : Int, val dataBits : Int) extends Bundle {
  val waitrequest = Output(Bool())
  val read  = Input(Bool())
  val readdata = Output(UInt(dataBits.W))
  val address = Input(UInt(addrBits.W))
  val write  = Input(Bool())
  val writedata = Input(UInt(dataBits.W))
}

class MemBlock(val addrBits : Int, val dataBits : Int, val bypass : Boolean = true) extends Module {
  val io = IO(new MemBlockIO(addrBits, dataBits))
  val mem = Mem(1 << addrBits, UInt(dataBits.W))

  // write
  when (io.write === 1.U) {
    mem(io.address) := io.writedata
  }

  // read
  val addrreg = RegNext(io.address)
  io.readdata := mem(addrreg)
  io.waitrequest := 0.U

  if (bypass) {
    // force read during write behavior
    when (RegNext(io.write) === 1.U && RegNext(io.address) === addrreg) {
      io.readdata := RegNext(io.writedata)
    }
  }
}

