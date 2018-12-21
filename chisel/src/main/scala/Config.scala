// See LICENSE for license details.

package vta

import chisel3.Module
import freechips.rocketchip.config.{Parameters, Config}
import junctions._

class VTAConfig extends Config((site, here, up) => {
  // Core
  case XLEN => 8
  case LOG_INP_WIDTH => 3
  case LOG_WGT_WIDTH => 3
  case LOG_ACC_WIDTH => 5
  case LOG_OUT_WIDTH => 3
  case LOG_BATCH => 0
  case LOG_BLOCK_IN => 4
  case LOG_BLOCK_OUT => 4
  case BuildALU    => (p: Parameters) => Module(new ALU()(p))

  case OPCODE_BIT_WIDTH => 3

  case ALU_OPCODE_MIN => 0
  case ALU_OPCODE_MAX => 1
  case ALU_OPCODE_ADD => 2
  case ALU_OPCODE_SHR => 3

  case OPCODE_LOAD => 0
  case OPCODE_STORE => 1
  case OPCODE_GEMM => 2
  case OPCODE_FINISH => 3
  case OPCODE_ALU => 4

  // NastiIO
  case NastiKey => new NastiParameters(
    idBits   = 5,
    dataBits = 64,
    addrBits = here(XLEN))
}
)
