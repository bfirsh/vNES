public class Mapper140 extends MapperDefault {

    public void init(NES nes) {

        super.init(nes);

    }

        public void loadROM(ROM rom) {

        if (!rom.isValid()) {
            //System.out.println("Invalid ROM! Unable to load.");
            return;
        }

        // Initial Load:
        loadPRGROM();
        loadCHRROM();

        // Do Reset-Interrupt:
        nes.getCpu().requestIrq(CPU.IRQ_RESET);

    }

    public void write(int address, short value) {

        if (address < 0x8000) {
            // Handle normally:
            super.write(address, value);
        } 

        if (address >= 0x6000 && address < 0x8000) {
            int prg_bank = (value & 0xF0) >> 4;
            int chr_bank = value & 0x0F;

            load32kRomBank(prg_bank, 0x8000);
            load8kVromBank(chr_bank, 0x0000);
        }
    }
}