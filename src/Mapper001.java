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

public class Mapper001 extends MapperDefault {

    // Register flags:

    // Register 0:
    int mirroring;
    int oneScreenMirroring;
    int prgSwitchingArea = 1;
    int prgSwitchingSize = 1;
    int vromSwitchingSize;

    // Register 1:
    int romSelectionReg0;

    // Register 2:
    int romSelectionReg1;

    // Register 3:
    int romBankSelect;

    // 5-bit buffer:
    int regBuffer;
    int regBufferCounter;

    public void init(NES nes) {

        super.init(nes);

    }

    public void mapperInternalStateLoad(ByteBuffer buf) {

        // Check version:
        if (buf.readByte() == 1) {

            // Reg 0:
            mirroring = buf.readInt();
            oneScreenMirroring = buf.readInt();
            prgSwitchingArea = buf.readInt();
            prgSwitchingSize = buf.readInt();
            vromSwitchingSize = buf.readInt();

            // Reg 1:
            romSelectionReg0 = buf.readInt();

            // Reg 2:
            romSelectionReg1 = buf.readInt();

            // Reg 3:
            romBankSelect = buf.readInt();

            // 5-bit buffer:
            regBuffer = buf.readInt();
            regBufferCounter = buf.readInt();

        }

    }

    public void mapperInternalStateSave(ByteBuffer buf) {

        // Version:
        buf.putByte((short) 1);

        // Reg 0:
        buf.putInt(mirroring);
        buf.putInt(oneScreenMirroring);
        buf.putInt(prgSwitchingArea);
        buf.putInt(prgSwitchingSize);
        buf.putInt(vromSwitchingSize);

        // Reg 1:
        buf.putInt(romSelectionReg0);

        // Reg 2:
        buf.putInt(romSelectionReg1);

        // Reg 3:
        buf.putInt(romBankSelect);

        // 5-bit buffer:
        buf.putInt(regBuffer);
        buf.putInt(regBufferCounter);

    }

    public void write(int address, short value) {

        // Writes to addresses other than MMC registers are handled by NoMapper.
        if (address < 0x8000) {
            super.write(address, value);
            return;
        }

        ////System.out.println("MMC Write. Reg="+(getRegNumber(address))+" Value="+value);

        // See what should be done with the written value:
        if ((value & 128) != 0) {

            // Reset buffering:
            regBufferCounter = 0;
            regBuffer = 0;

            // Reset register:
            if (getRegNumber(address) == 0) {

                prgSwitchingArea = 1;
                prgSwitchingSize = 1;

            }

        } else {

            // Continue buffering:
            //regBuffer = (regBuffer & (0xFF-(1<<regBufferCounter))) | ((value & (1<<regBufferCounter))<<regBufferCounter);
            regBuffer = (regBuffer & (0xFF - (1 << regBufferCounter))) | ((value & 1) << regBufferCounter);
            regBufferCounter++;
            if (regBufferCounter == 5) {

                // Use the buffered value:
                setReg(getRegNumber(address), regBuffer);

                // Reset buffer:
                regBuffer = 0;
                regBufferCounter = 0;

            }

        }

    }

    private void setReg(int reg, int value) {

        int tmp, tmp2;

        if (reg == 0) {

            // Mirroring:
            tmp = value & 3;
            if (tmp != mirroring) {
                // Set mirroring:
                mirroring = tmp;
                if ((mirroring & 2) == 0) {
                    // SingleScreen mirroring overrides the other setting:
                    ////System.out.println("MMC1: Setting Singlescreen Mirroring.");
                    nes.getPpu().setMirroring(ROM.SINGLESCREEN_MIRRORING);
                } else {
                    // Not overridden by SingleScreen mirroring.
                    ////System.out.println("MMC1: Setting Normal Mirroring. value="+mirroring);
                    nes.getPpu().setMirroring((mirroring & 1) != 0 ? ROM.HORIZONTAL_MIRRORING : ROM.VERTICAL_MIRRORING);
                }
            }

            // PRG Switching Area;
            prgSwitchingArea = (value >> 2) & 1;

            // PRG Switching Size:
            prgSwitchingSize = (value >> 3) & 1;

            // VROM Switching Size:
            vromSwitchingSize = (value >> 4) & 1;

        } else if (reg == 1) {

            // ROM selection:
            romSelectionReg0 = (value >> 4) & 1;

            // Check whether the cart has VROM:
            if (nes.getRom().getVromBankCount() > 0) {

                // Select VROM bank at 0x0000:
                if (vromSwitchingSize == 0) {

                    // Swap 8kB VROM:
                    ////System.out.println("Swapping 8k VROM, bank="+(value&0xF)+" romSelReg="+romSelectionReg0);
                    if (romSelectionReg0 == 0) {
                        load8kVromBank((value & 0xF), 0x0000);
                    } else {
                        load8kVromBank(nes.getRom().getVromBankCount() / 2 + (value & 0xF), 0x0000);
                    }

                } else {

                    // Swap 4kB VROM:
                    ////System.out.println("ROMSELREG0 = "+romSelectionReg0);
                    ////System.out.println("Swapping 4k VROM at 0x0000, bank="+(value&0xF));

                    if (romSelectionReg0 == 0) {
                        loadVromBank((value & 0xF), 0x0000);
                    } else {
                        loadVromBank(nes.getRom().getVromBankCount() / 2 + (value & 0xF), 0x0000);
                    }

                }

            }

        } else if (reg == 2) {

            // ROM selection:
            romSelectionReg1 = (value >> 4) & 1;

            // Check whether the cart has VROM:
            if (nes.getRom().getVromBankCount() > 0) {

                // Select VROM bank at 0x1000:
                if (vromSwitchingSize == 1) {

                    // Swap 4kB of VROM:
                    ////System.out.println("ROMSELREG1 = "+romSelectionReg1);
                    ////System.out.println("Swapping 4k VROM at 0x1000, bank="+(value&0xF));
                    if (romSelectionReg1 == 0) {
                        loadVromBank((value & 0xF), 0x1000);
                    } else {
                        loadVromBank(nes.getRom().getVromBankCount() / 2 + (value & 0xF), 0x1000);
                    }

                }

            }

        } else {

            // Select ROM bank:
            // -------------------------
            tmp = value & 0xF;
            int bank;
            int baseBank = 0;
            int bankCount = nes.getRom().getRomBankCount();

            if (bankCount >= 32) {

                // 1024 kB cart
                if (vromSwitchingSize == 0) {
                    if (romSelectionReg0 == 1) {
                        baseBank = 16;
                    }
                } else {
                    baseBank = (romSelectionReg0 | (romSelectionReg1 << 1)) << 3;
                }

            } else if (bankCount >= 16) {

                // 512 kB cart
                if (romSelectionReg0 == 1) {
                    baseBank = 8;
                }

            }

            if (prgSwitchingSize == 0) {

                // 32kB
                bank = baseBank + (value & 0xF);
                load32kRomBank(bank, 0x8000);

            } else {

                // 16kB
                bank = baseBank * 2 + (value & 0xF);
                if (prgSwitchingArea == 0) {
                    loadRomBank(bank, 0xC000);
                } else {
                    loadRomBank(bank, 0x8000);
                }

            }

        // -------------------------

        }

    }

    // Returns the register number from the address written to:
    private int getRegNumber(int address) {

        if (address >= 0x8000 && address <= 0x9FFF) {
            return 0;
        } else if (address >= 0xA000 && address <= 0xBFFF) {
            return 1;
        } else if (address >= 0xC000 && address <= 0xDFFF) {
            return 2;
        } else {
            return 3;
        }

    }

    public void loadROM(ROM rom) {

        //System.out.println("Loading ROM.");

        if (!rom.isValid()) {
            //System.out.println("MMC1: Invalid ROM! Unable to load.");
            return;
        }

        // Load PRG-ROM:
        loadRomBank(0, 0x8000);				//   First ROM bank..
        loadRomBank(rom.getRomBankCount() - 1, 0xC000); 	// ..and last ROM bank.

        // Load CHR-ROM:
        loadCHRROM();

        // Load Battery RAM (if present):
        loadBatteryRam();

        // Do Reset-Interrupt:
        //nes.getCpu().doResetInterrupt();
        nes.getCpu().requestIrq(CPU.IRQ_RESET);

    }

    public void reset() {

        regBuffer = 0;
        regBufferCounter = 0;

        // Register 0:
        mirroring = 0;
        oneScreenMirroring = 0;
        prgSwitchingArea = 1;
        prgSwitchingSize = 1;
        vromSwitchingSize = 0;

        // Register 1:
        romSelectionReg0 = 0;

        // Register 2:
        romSelectionReg1 = 0;

        // Register 3:
        romBankSelect = 0;

    }

    private void switchLowHighPrgRom(int oldSetting) {

        // not yet.
    }

    private void switch16to32() {

        // not yet.
    }

    private void switch32to16() {

        // not yet.
    }
}