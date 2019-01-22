// See LICENSE for license details.

package vta

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters

class MemArbiterIO(implicit p: Parameters) extends CoreBundle()(p) {
  val ins_cache = new AvalonSlaveIO(dataBits = 128, addrBits = 32)
  val inp_cache = new AvalonSlaveIO(dataBits = 128, addrBits = 32)
  val wgt_cache = new AvalonSlaveIO(dataBits = 128, addrBits = 32)
  val acc_cache = new AvalonSlaveIO(dataBits = 128, addrBits = 32)
  val out_cache = new AvalonSlaveIO(dataBits = 128, addrBits = 32)
  val axi_master = Flipped(new AvalonSlaveIO(dataBits = 128, addrBits = 32))
}

class MemArbiter(implicit val p: Parameters) extends Module {
  val io = IO(new MemArbiterIO())

  val s_IDLE :: s_INS_CACHE_READ :: s_INP_CACHE_READ :: s_WGT_CACHE_READ :: s_ACC_CACHE_READ :: s_OUT_CACHE_WRITE :: s_OUT_CACHE_ACK :: Nil = Enum(7)
  val state = RegInit(s_IDLE)

  // write
  io.axi_master.address := io.out_cache.address
  io.axi_master.writedata  := io.out_cache.writedata
  io.axi_master.write := io.out_cache.write && state === s_OUT_CACHE_WRITE
  io.out_cache.waitrequest := io.axi_master.waitrequest

  // read
  io.axi_master.address := MuxLookup(state, s_IDLE,
    List(s_INS_CACHE_READ -> io.ins_cache.address,
         s_INP_CACHE_READ -> io.inp_cache.address,
         s_WGT_CACHE_READ -> io.wgt_cache.address,
         s_ACC_CACHE_READ -> io.acc_cache.address, s_IDLE -> 0.U))
  io.ins_cache.readdata := io.axi_master.readdata
  io.ins_cache.read := io.axi_master.read && state === s_INS_CACHE_READ
  io.ins_cache.waitrequest := io.axi_master.waitrequest && state === s_INS_CACHE_READ
  io.inp_cache.readdata := io.axi_master.readdata
  io.inp_cache.read := io.axi_master.read && state === s_INP_CACHE_READ
  io.inp_cache.waitrequest := io.axi_master.waitrequest && state === s_INP_CACHE_READ
  io.wgt_cache.readdata := io.axi_master.readdata
  io.wgt_cache.read := io.axi_master.read && state === s_WGT_CACHE_READ
  io.wgt_cache.waitrequest := io.axi_master.waitrequest && state === s_WGT_CACHE_READ
  io.acc_cache.readdata := io.axi_master.readdata
  io.acc_cache.read := io.axi_master.read && state === s_ACC_CACHE_READ
  io.acc_cache.waitrequest := io.axi_master.waitrequest && state === s_ACC_CACHE_READ

  switch(state) {
    is(s_IDLE) {
    }
  }

}
