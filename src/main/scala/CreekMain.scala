package Creek

import Chisel._

object CreekMain {
    def main(args: Array[String]) {
        val testArgs = args.slice(1, args.length)
        args(0) match {
            case "RegisterSet" => chiselMainTest(testArgs,
                () => Module(new RegisterSet(256, 32))) {
                    c => new RegisterSetTest(c)
                }
            case "AdderUnit" => chiselMainTest(testArgs,
                () => Module(new AdderUnit(4, 256))) {
                    c => new AdderUnitTest(c)
                }
            case "MultiplierUnit" => chiselMainTest(testArgs,
                () => Module(new MultiplierUnit(4, 256))) {
                    c => new MultiplierUnitTest(c)
                }
        }
    }
}
