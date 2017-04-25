
public class Mapper015 extends MapperDefault {

    public void init(NES nes) {
        super.init(nes);
    }

    public void write(int address, short value) {

        if (address < 0x8000) {
            super.write(address, value);
        } else {
            switch (address) {

                case 0x8000:
                     {
                        if ((value & 0x80) != 0) {
                            load8kRomBank((value & 0x3F) * 2 + 1, 0x8000);
                            load8kRomBank((value & 0x3F) * 2 + 0, 0xA000);
                            load8kRomBank((value & 0x3F) * 2 + 3, 0xC000);
                            load8kRomBank((value & 0x3F) * 2 + 2, 0xE000);
                        } else {
                            load8kRomBank((value & 0x3F) * 2 + 0, 0x8000);
                            load8kRomBank((value & 0x3F) * 2 + 1, 0xA000);
                            load8kRomBank((value & 0x3F) * 2 + 2, 0xC000);
                            load8kRomBank((value & 0x3F) * 2 + 3, 0xE000);
                        }
                        if ((value & 0x40) != 0) {
                            nes.getPpu().setMirroring(ROM.HORIZONTAL_MIRRORING);
                        } else {
                            nes.getPpu().setMirroring(ROM.VERTICAL_MIRRORING);
                        }
                    }
                    break;
                case 0x8001:
                     {
                        if ((value & 0x80) != 0) {
                            load8kRomBank((value & 0x3F) * 2 + 1, 0xC000);
                            load8kRomBank((value & 0x3F) * 2 + 0, 0xE000);
                        } else {
                            load8kRomBank((value & 0x3F) * 2 + 0, 0xC000);
                            load8kRomBank((value & 0x3F) * 2 + 1, 0xE000);
                        }
                    }
                    break;
                case 0x8002:
                     {
                        if ((value & 0x80) != 0) {
                            load8kRomBank((value & 0x3F) * 2 + 1, 0x8000);
                            load8kRomBank((value & 0x3F) * 2 + 1, 0xA000);
                            load8kRomBank((value & 0x3F) * 2 + 1, 0xC000);
                            load8kRomBank((value & 0x3F) * 2 + 1, 0xE000);
                        } else {
                            load8kRomBank((value & 0x3F) * 2, 0x8000);
                            load8kRomBank((value & 0x3F) * 2, 0xA000);
                            load8kRomBank((value & 0x3F) * 2, 0xC000);
                            load8kRomBank((value & 0x3F) * 2, 0xE000);
                        }
                    }
                    break;
                case 0x8003:
                     {
                        if ((value & 0x80) != 0) {
                            load8kRomBank((value & 0x3F) * 2 + 1, 0xC000);
                            load8kRomBank((value & 0x3F) * 2 + 0, 0xE000);
                        } else {
                            load8kRomBank((value & 0x3F) * 2 + 0, 0xC000);
                            load8kRomBank((value & 0x3F) * 2 + 1, 0xE000);
                        }
                        if ((value & 0x40) != 0) {
                            nes.getPpu().setMirroring(ROM.HORIZONTAL_MIRRORING);
                        } else {
                            nes.getPpu().setMirroring(ROM.VERTICAL_MIRRORING);
                        }
                    }
                    break;
            }
        }
    }

    public void loadROM(ROM rom) {

        if (!rom.isValid()) {
            System.out.println("015: Invalid ROM! Unable to load.");
            return;

        }

        // Load PRG-ROM:
        load8kRomBank(0, 0x8000);
        load8kRomBank(1, 0xA000);
        load8kRomBank(2, 0xC000);
        load8kRomBank(3, 0xE000);

        // Load CHR-ROM:
        loadCHRROM();

        // Load Battery RAM (if present):
        loadBatteryRam();

        // Do Reset-Interrupt:
        nes.getCpu().requestIrq(CPU.IRQ_RESET);
    }
}