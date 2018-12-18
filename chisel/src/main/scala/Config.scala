// See LICENSE for license details.

package vta

import chisel3.Module
import freechips.rocketchip.config.{Parameters, Config}
import junctions._

class VTAConfig extends Config((site, here, up) => {
    // Core
    case XLEN => 8
    case BuildALU    => (p: Parameters) => Module(new ALUSimple()(p))
    // NastiIO
    case NastiKey => new NastiParameters(
      idBits   = 5,
      dataBits = 64,
      addrBits = here(XLEN))
  }
)
