UNITS = RegisterSet AdderUnit MultiplierUnit AdderSetup MultiplierSetup \
	MemoryTest MemoryControllerRegisters \
	Datapath SwitchController CreekController CreekCoreSetup

test-all: $(addsuffix .vcd, $(UNITS))

fpga: CreekCore.v

%.vcd: src/main/scala/%.scala
	sbt "run $(notdir $(basename $<)) --debug --genHarness --compile --test --vcd --backend c"

%.v: src/main/scala/%.scala
	sbt "run $(notdir $(basename $<)) --compile --backend fpga"

clean:
	rm -f $(UNITS) *.vcd *.v *.cpp *.o *.h
