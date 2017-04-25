
public class Mapper094 extends MapperDefault {

    public void init(NES nes) {
        super.init(nes);
    }

    public void write(int address, short value) {

        if (address < 0x8000) {

            // Let the base mapper take care of it.
            super.write(address, value);

        } else {

            if ((address & 0xFFF0) == 0xFF00) {
                int bank = (value & 0x1C) >> 2;
                loadRomBank(bank, 0x8000);
            }
        }
    }

    public void loadROM(ROM rom) {

        int num_banks = rom.getRomBankCount();

        // Load PRG-ROM:
        loadRomBank(0, 0x8000);
        loadRomBank(num_banks - 1, 0xC000);

        // Load CHR-ROM:
        loadCHRROM();

        // Do Reset-Interrupt:
        nes.getCpu().requestIrq(CPU.IRQ_RESET);

    }
}
