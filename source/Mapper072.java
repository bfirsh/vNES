
public class Mapper072 extends MapperDefault {

    public void init(NES nes) {
        super.init(nes);
    }

    public void write(int address, short value) {

        if (address < 0x8000) {
            super.write(address, value);
        } else {
            int bank = value & 0x0f;
            int num_banks = rom.getRomBankCount();

            if ((value & 0x80) != 0) {
                loadRomBank(bank * 2, 0x8000);
                loadRomBank(num_banks - 1, 0xC000);
            }
            if ((value & 0x40) != 0) {
                load8kVromBank(bank * 8, 0x0000);
            }
        }
    }

    public void loadROM(ROM rom) {

        if (!rom.isValid()) {
            System.out.println("048: Invalid ROM! Unable to load.");
            return;
        }

        // Get number of 8K banks:
        int num_banks = rom.getRomBankCount() * 2;

        // Load PRG-ROM:
        loadRomBank(1, 0x8000);
        loadRomBank(num_banks - 1, 0xC000);

        // Load CHR-ROM:
        loadCHRROM();

        // Load Battery RAM (if present):
        // loadBatteryRam();

        // Do Reset-Interrupt:
        nes.getCpu().requestIrq(CPU.IRQ_RESET);
    }
}