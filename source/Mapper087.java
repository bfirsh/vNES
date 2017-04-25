
public class Mapper087 extends MapperDefault {

    public void init(NES nes) {
        super.init(nes);
    }

    public void writelow(int address, short value) {

        if (address < 0x6000) {
            // Let the base mapper take care of it.
            super.writelow(address, value);
        } else if (address == 0x6000) {
            int chr_bank = (value & 0x02) >> 1;
            load8kVromBank(chr_bank * 8, 0x0000);
        }
    }

    public void loadROM(ROM rom) {

        if (!rom.isValid()) {
            System.out.println("Invalid ROM! Unable to load.");
            return;
        }

        // Get number of 8K banks:
        int num_8k_banks = rom.getRomBankCount() * 2;

        // Load PRG-ROM:
        load8kRomBank(0, 0x8000);
        load8kRomBank(1, 0xA000);
        load8kRomBank(2, 0xC000);
        load8kRomBank(3, 0xE000);

        // Load CHR-ROM:
        loadCHRROM();

        // Load Battery RAM (if present):

        // Do Reset-Interrupt:
        nes.getCpu().requestIrq(CPU.IRQ_RESET);
    }
}
