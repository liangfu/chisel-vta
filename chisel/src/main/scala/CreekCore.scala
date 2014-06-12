package Creek

import Chisel._
import Creek.Constants.FloatSize

class CreekCore(
        instr_depth: Int, lanes: Int,
        regdepth: Int, nregs: Int,
        memaddrsize: Int) extends Module {

    val VectorWidth = FloatSize * lanes

    val io = new Bundle {
        val pause_n = Bool(INPUT)
        val local_init_done = Bool(INPUT)
        val avl_waitrequest_n = Bool(INPUT)
        val avl_address = UInt(OUTPUT, memaddrsize)
        val avl_readdatavalid = Bool(INPUT)
        val avl_readdata = UInt(INPUT, VectorWidth)
        val avl_writedata = UInt(OUTPUT, VectorWidth)
        val avl_read = Bool(OUTPUT)
        val avl_write = Bool(OUTPUT)

        val instr_address = UInt(OUTPUT, log2Up(instr_depth))
        val instr_data = UInt(INPUT, 16)
    }

    val datapath = Module(new Datapath(lanes, regdepth, nregs, memaddrsize))
    datapath.io.local_init_done := io.local_init_done
    datapath.io.avl_waitrequest_n := io.avl_waitrequest_n
    io.avl_address := datapath.io.avl_address
    datapath.io.avl_readdatavalid := io.avl_readdatavalid
    datapath.io.avl_readdata := io.avl_readdata
    io.avl_writedata := datapath.io.avl_writedata
    io.avl_read := datapath.io.avl_read
    io.avl_write := datapath.io.avl_write

    val controller = Module(new CreekController(instr_depth, nregs))
    controller.io.pause_n := io.pause_n
    controller.io.local_init_done := io.local_init_done
    controller.io.instr_data := io.instr_data
    io.instr_address := controller.io.instr_address

    datapath.io.input_select := controller.io.input_select
    datapath.io.output_select := controller.io.output_select
    controller.io.reg_read_busy := datapath.io.reg_read_busy
    controller.io.reg_write_busy := datapath.io.reg_write_busy
    datapath.io.reg_copy_reset := controller.io.reg_copy_reset
    datapath.io.adder_reset := controller.io.adder_reset
    controller.io.adder_busy := datapath.io.adder_busy
    datapath.io.adder_use_scalar := controller.io.adder_use_scalar
    datapath.io.adder_subtract := controller.io.adder_subtract
    datapath.io.mult_reset := controller.io.mult_reset
    controller.io.mult_busy := datapath.io.mult_busy
    datapath.io.mult_use_scalar := controller.io.mult_use_scalar
    datapath.io.mult_square := controller.io.mult_square
    controller.io.mem_ready := datapath.io.mem_ready
    datapath.io.mem_start_read := controller.io.mem_start_read
    datapath.io.mem_start_write := controller.io.mem_start_write
    datapath.io.scalar_address := controller.io.scalar_address
    datapath.io.scalar_writedata := controller.io.scalar_writedata
    datapath.io.scalar_byteenable := controller.io.scalar_byteenable
    datapath.io.scalar_write := controller.io.scalar_write
}