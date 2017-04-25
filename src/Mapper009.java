/*
vNES
Copyright © 2006-2010 Jamie Sanders

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class Mapper009 extends MapperDefault {

    int latchLo;
    int latchHi;
    int latchLoVal1;
    int latchLoVal2;
    int latchHiVal1;
    int latchHiVal2;

    public void init(NES nes) {

        super.init(nes);
        reset();

    }

    public void write(int address, short value) {

        if (address < 0x8000) {

            // Handle normally.
            super.write(address, value);

        } else {

            // MMC2 write.

            value &= 0xFF;
            address &= 0xF000;
            switch (address >> 12) {
                case 0xA: {

                    // Select 8k ROM bank at 0x8000
                    load8kRomBank(value, 0x8000);
                    return;

                }
                case 0xB: {

                    // Select 4k VROM bank at 0x0000, $FD mode
                    latchLoVal1 = value;
                    if (latchLo == 0xFD) {
                        loadVromBank(value, 0x0000);
                    }
                    return;

                }
                case 0xC: {

                    // Select 4k VROM bank at 0x0000, $FE mode
                    latchLoVal2 = value;
                    if (latchLo == 0xFE) {
                        loadVromBank(value, 0x0000);
                    }
                    return;

                }
                case 0xD: {

                    // Select 4k VROM bank at 0x1000, $FD mode
                    latchHiVal1 = value;
                    if (latchHi == 0xFD) {
                        loadVromBank(value, 0x1000);
                    }
                    return;

                }
                case 0xE: {

                    // Select 4k VROM bank at 0x1000, $FE mode
                    latchHiVal2 = value;
                    if (latchHi == 0xFE) {
                        loadVromBank(value, 0x1000);
                    }
                    return;

                }
                case 0xF: {

                    // Select mirroring
                    if ((value & 0x1) == 0) {

                        // Vertical mirroring
                        nes.getPpu().setMirroring(ROM.VERTICAL_MIRRORING);

                    } else {

                        // Horizontal mirroring
                        nes.getPpu().setMirroring(ROM.HORIZONTAL_MIRRORING);

                    }
                    return;
                }
            }
        }
    }

    public void loadROM(ROM rom) {

        //System.out.println("Loading ROM.");

        if (!rom.isValid()) {
            //System.out.println("MMC2: Invalid ROM! Unable to load.");
            return;
        }

        // Get number of 8K banks:
        int num_8k_banks = rom.getRomBankCount() * 2;

        // Load PRG-ROM:
        load8kRomBank(0, 0x8000);
        load8kRomBank(num_8k_banks - 3, 0xA000);
        load8kRomBank(num_8k_banks - 2, 0xC000);
        load8kRomBank(num_8k_banks - 1, 0xE000);

        // Load CHR-ROM:
        loadCHRROM();

        // Load Battery RAM (if present):
        loadBatteryRam();

        // Do Reset-Interrupt:
        nes.getCpu().requestIrq(CPU.IRQ_RESET);

    }

    public void latchAccess(int address) {
        if ((address & 0x1FF0) == 0x0FD0 && latchLo != 0xFD) {
            // Set $FD mode
            loadVromBank(latchLoVal1, 0x0000);
            latchLo = 0xFD;
        //System.out.println("LO FD");
        } else if ((address & 0x1FF0) == 0x0FE0 && latchLo != 0xFE) {
            // Set $FE mode
            loadVromBank(latchLoVal2, 0x0000);
            latchLo = 0xFE;
        //System.out.println("LO FE");
        } else if ((address & 0x1FF0) == 0x1FD0 && latchHi != 0xFD) {
            // Set $FD mode
            loadVromBank(latchHiVal1, 0x1000);
            latchHi = 0xFD;
        //System.out.println("HI FD");
        } else if ((address & 0x1FF0) == 0x1FE0 && latchHi != 0xFE) {
            // Set $FE mode
            loadVromBank(latchHiVal2, 0x1000);
            latchHi = 0xFE;
        //System.out.println("HI FE");
        }
    }

    public void mapperInternalStateLoad(ByteBuffer buf) {

        super.mapperInternalStateLoad(buf);

        // Check version:
        if (buf.readByte() == 1) {

            latchLo = buf.readByte();
            latchHi = buf.readByte();
            latchLoVal1 = buf.readByte();
            latchLoVal2 = buf.readByte();
            latchHiVal1 = buf.readByte();
            latchHiVal2 = buf.readByte();

        }

    }

    public void mapperInternalStateSave(ByteBuffer buf) {

        super.mapperInternalStateSave(buf);

        // Version:
        buf.putByte((short) 1);

        // State:
        buf.putByte((byte) latchLo);
        buf.putByte((byte) latchHi);
        buf.putByte((byte) latchLoVal1);
        buf.putByte((byte) latchLoVal2);
        buf.putByte((byte) latchHiVal1);
        buf.putByte((byte) latchHiVal2);

    }

    public void reset() {

        // Set latch to $FE mode:
        latchLo = 0xFE;
        latchHi = 0xFE;
        latchLoVal1 = 0;
        latchLoVal2 = 4;
        latchHiVal1 = 0;
        latchHiVal2 = 0;

    }
}