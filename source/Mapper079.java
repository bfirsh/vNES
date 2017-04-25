
public class Mapper079 extends MapperDefault {

    public void init(NES nes) {

        super.init(nes);

    }

    public void writelow(int address, short value) {

        if (address < 0x4000) {
            super.writelow(address, value);
        } 
        
        if (address < 0x6000 & address >= 0x4100) {
            int prg_bank = (value & 0x08) >> 3;
            int chr_bank = value & 0x07;

            load32kRomBank(prg_bank, 0x8000);
            load8kVromBank(chr_bank, 0x0000);
        }

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

}