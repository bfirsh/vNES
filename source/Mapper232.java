
public class Mapper232 extends MapperDefault {

    int regs232[] = new int[2];

    public void init(NES nes) {

        super.init(nes);
        reset();

    }

    public void write(int address, short value) {

        if (address < 0x8000) {

            // Handle normally:
            super.write(address, value);

        } else if (address == 0x9000) {

            regs232[0] = (value & 0x18) >> 1;
        } else if (0xA000 <= address && address <= 0xFFFF) {
            regs232[1] = value & 0x03;
        }

        loadRomBank((regs232[0] | regs232[1]), 0x8000);
        loadRomBank((regs232[0] | 0x03), 0xC000);
    }

      public void loadROM(ROM rom) {

        //System.out.println("Loading ROM.");

        if (!rom.isValid()) {
            //System.out.println("Camerica: Invalid ROM! Unable to load.");
            return;
        }

        // Get number of PRG ROM banks:
        int num_banks = rom.getRomBankCount();

        // Load PRG-ROM:
        loadRomBank((regs232[0] | regs232[1]), 0x8000);
        loadRomBank((regs232[0] | 0x03), 0xC000);

        // Load CHR-ROM:
        loadCHRROM();

        // Load Battery RAM (if present):
        loadBatteryRam();

        // Do Reset-Interrupt:
        nes.getCpu().requestIrq(CPU.IRQ_RESET);

    }

    public void reset() {

        regs232[0] = 0x0C;
        regs232[1] = 0x00;

    }
}
