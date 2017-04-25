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

public class MapperDefault implements MemoryMapper {

    public NES nes;
    public Memory cpuMem;
    public Memory ppuMem;
    public short[] cpuMemArray;
    public ROM rom;
    public CPU cpu;
    public PPU ppu;
    public int cpuMemSize;
    public int joy1StrobeState;
    public int joy2StrobeState;
    public int joypadLastWrite;
    public boolean mousePressed;
    public boolean gameGenieActive;
    public int mouseX;
    public int mouseY;
    int tmp;
    long crc;

    public void init(NES nes) {

        this.nes = nes;
        this.cpuMem = nes.getCpuMemory();
        this.cpuMemArray = cpuMem.mem;
        this.ppuMem = nes.getPpuMemory();
        this.rom = nes.getRom();
        this.cpu = nes.getCpu();
        this.ppu = nes.getPpu();

        cpuMemSize = cpuMem.getMemSize();
        joypadLastWrite = -1;

    }

    public void stateLoad(ByteBuffer buf) {

        // Check version:
        if (buf.readByte() == 1) {

            // Joypad stuff:
            joy1StrobeState = buf.readInt();
            joy2StrobeState = buf.readInt();
            joypadLastWrite = buf.readInt();

            // Mapper specific stuff:
            mapperInternalStateLoad(buf);

        }

    }

    public void stateSave(ByteBuffer buf) {

        // Version:
        buf.putByte((short) 1);

        // Joypad stuff:
        buf.putInt(joy1StrobeState);
        buf.putInt(joy2StrobeState);
        buf.putInt(joypadLastWrite);

        // Mapper specific stuff:
        mapperInternalStateSave(buf);

    }

    public void mapperInternalStateLoad(ByteBuffer buf) {

        buf.putByte((short) joy1StrobeState);
        buf.putByte((short) joy2StrobeState);
        buf.putByte((short) joypadLastWrite);

    }

    public void mapperInternalStateSave(ByteBuffer buf) {

        joy1StrobeState = buf.readByte();
        joy2StrobeState = buf.readByte();
        joypadLastWrite = buf.readByte();

    }

    public void setGameGenieState(boolean enable) {
        gameGenieActive = enable;
    }

    public boolean getGameGenieState() {
        return gameGenieActive;
    }

    public void write(int address, short value) {

        if (address < 0x2000) {

            // Mirroring of RAM:
            cpuMem.mem[address & 0x7FF] = value;

        } else if (address > 0x4017) {

            cpuMem.mem[address] = value;
            if (address >= 0x6000 && address < 0x8000) {

                // Write to SaveRAM. Store in file:
                if (rom != null) {
                    rom.writeBatteryRam(address, value);
                }

            }

        } else if (address > 0x2007 && address < 0x4000) {

            regWrite(0x2000 + (address & 0x7), value);

        } else {

            regWrite(address, value);

        }

    }

    public void writelow(int address, short value) {

        if (address < 0x2000) {
            // Mirroring of RAM:
            cpuMem.mem[address & 0x7FF] = value;

        } else if (address > 0x4017) {
            cpuMem.mem[address] = value;

        } else if (address > 0x2007 && address < 0x4000) {
            regWrite(0x2000 + (address & 0x7), value);

        } else {
            regWrite(address, value);
        }

    }

    public short load(int address) {

        // Game Genie codes active?
        if (gameGenieActive) {
            if (nes.gameGenie.addressMatch[address]) {

                tmp = nes.gameGenie.getCodeIndex(address);

                // Check the code type:
                if (nes.gameGenie.getCodeType(tmp) == GameGenie.TYPE_6CHAR) {

                    // Return the code value:
                    return (short) nes.gameGenie.getCodeValue(tmp);

                } else {

                    // Check whether the actual value equals the compare value:
                    if (cpuMemArray[address] == nes.gameGenie.getCodeCompare(tmp)) {

                        // The values match, so use the supplied game genie value:
                        return (short) nes.gameGenie.getCodeValue(tmp);

                    }

                }
            }
        }

        // Wrap around:
        address &= 0xFFFF;

        // Check address range:
        if (address > 0x4017) {

            // ROM:
            return cpuMemArray[address];

        } else if (address >= 0x2000) {

            // I/O Ports.
            return regLoad(address);

        } else {

            // RAM (mirrored)
            return cpuMemArray[address & 0x7FF];

        }

    }

    public short regLoad(int address) {

        switch (address >> 12) { // use fourth nibble (0xF000)

            case 0: {
                break;
            }
            case 1: {
                break;
            }
            case 2: {
                // Fall through to case 3
            }
            case 3: {

                // PPU Registers
                switch (address & 0x7) {
                    case 0x0: {

                        // 0x2000:
                        // PPU Control Register 1.
                        // (the value is stored both
                        // in main memory and in the
                        // PPU as flags):
                        // (not in the real NES)
                        return cpuMem.mem[0x2000];

                    }
                    case 0x1: {

                        // 0x2001:
                        // PPU Control Register 2.
                        // (the value is stored both
                        // in main memory and in the
                        // PPU as flags):
                        // (not in the real NES)
                        return cpuMem.mem[0x2001];

                    }
                    case 0x2: {

                        // 0x2002:
                        // PPU Status Register.
                        // The value is stored in
                        // main memory in addition
                        // to as flags in the PPU.
                        // (not in the real NES)
                        return ppu.readStatusRegister();

                    }
                    case 0x3: {
                        return 0;
                    }
                    case 0x4: {

                        // 0x2004:
                        // Sprite Memory read.
                        return ppu.sramLoad();

                    }
                    case 0x5: {
                        return 0;
                    }
                    case 0x6: {
                        return 0;
                    }
                    case 0x7: {

                        // 0x2007:
                        // VRAM read:
                        return ppu.vramLoad();

                    }
                }
                break;

            }
            case 4: {


                // Sound+Joypad registers

                switch (address - 0x4015) {
                    case 0: {

                        // 0x4015:
                        // Sound channel enable, DMC Status
                        return nes.getPapu().readReg(address);

                    }
                    case 1: {

                        // 0x4016:
                        // Joystick 1 + Strobe
                        return joy1Read();

                    }
                    case 2: {

                        // 0x4017:
                        // Joystick 2 + Strobe
                        if (mousePressed && nes.ppu != null && nes.ppu.buffer != null) {

                            // Check for white pixel nearby:

                            int sx, sy, ex, ey, w;
                            sx = Math.max(0, mouseX - 4);
                            ex = Math.min(256, mouseX + 4);
                            sy = Math.max(0, mouseY - 4);
                            ey = Math.min(240, mouseY + 4);
                            w = 0;

                            for (int y = sy; y < ey; y++) {
                                for (int x = sx; x < ex; x++) {
                                    if ((nes.ppu.buffer[(y << 8) + x] & 0xFFFFFF) == 0xFFFFFF) {
                                        w = 0x1 << 3;
                                        break;
                                    }
                                }
                            }

                            w |= (mousePressed ? (0x1 << 4) : 0);
                            return (short) (joy2Read() | w);

                        } else {
                            return joy2Read();
                        }

                    }
                }

                break;

            }
        }

        return 0;

    }

    public void regWrite(int address, short value) {

        switch (address) {
            case 0x2000: {

                // PPU Control register 1
                cpuMem.write(address, value);
                ppu.updateControlReg1(value);
                break;

            }
            case 0x2001: {

                // PPU Control register 2
                cpuMem.write(address, value);
                ppu.updateControlReg2(value);
                break;

            }
            case 0x2003: {

                // Set Sprite RAM address:
                ppu.writeSRAMAddress(value);
                break;

            }
            case 0x2004: {

                // Write to Sprite RAM:
                ppu.sramWrite(value);
                break;

            }
            case 0x2005: {

                // Screen Scroll offsets:
                ppu.scrollWrite(value);
                break;

            }
            case 0x2006: {

                // Set VRAM address:
                ppu.writeVRAMAddress(value);
                break;

            }
            case 0x2007: {

                // Write to VRAM:
                ppu.vramWrite(value);
                break;

            }
            case 0x4014: {

                // Sprite Memory DMA Access
                ppu.sramDMA(value);
                break;

            }
            case 0x4015: {

                // Sound Channel Switch, DMC Status
                nes.getPapu().writeReg(address, value);
                break;

            }
            case 0x4016: {

                ////System.out.println("joy strobe write "+value);

                // Joystick 1 + Strobe
                if (value == 0 && joypadLastWrite == 1) {
                    ////System.out.println("Strobes reset.");
                    joy1StrobeState = 0;
                    joy2StrobeState = 0;
                }
                joypadLastWrite = value;
                break;

            }
            case 0x4017: {

                // Sound channel frame sequencer:
                nes.papu.writeReg(address, value);
                break;

            }
            default: {

                // Sound registers
                ////System.out.println("write to sound reg");
                if (address >= 0x4000 && address <= 0x4017) {
                    nes.getPapu().writeReg(address, value);
                }
                break;

            }
        }

    }

    public short joy1Read() {

        InputHandler in = nes.getGui().getJoy1();
        short ret;

        switch (joy1StrobeState) {
            case 0:
                ret = in.getKeyState(InputHandler.KEY_A);
                break;
            case 1:
                ret = in.getKeyState(InputHandler.KEY_B);
                break;
            case 2:
                ret = in.getKeyState(InputHandler.KEY_SELECT);
                break;
            case 3:
                ret = in.getKeyState(InputHandler.KEY_START);
                break;
            case 4:
                ret = in.getKeyState(InputHandler.KEY_UP);
                break;
            case 5:
                ret = in.getKeyState(InputHandler.KEY_DOWN);
                break;
            case 6:
                ret = in.getKeyState(InputHandler.KEY_LEFT);
                break;
            case 7:
                ret = in.getKeyState(InputHandler.KEY_RIGHT);
                break;
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
                ret = (short) 0;
                break;
            case 19:
                ret = (short) 1;
                break;
            default:
                ret = 0;
        }

        joy1StrobeState++;
        if (joy1StrobeState == 24) {
            joy1StrobeState = 0;
        }

        return ret;

    }

    public short joy2Read() {
        InputHandler in = nes.getGui().getJoy2();
        int st = joy2StrobeState;

        joy2StrobeState++;
        if (joy2StrobeState == 24) {
            joy2StrobeState = 0;
        }

        if (st == 0) {
            return in.getKeyState(InputHandler.KEY_A);
        } else if (st == 1) {
            return in.getKeyState(InputHandler.KEY_B);
        } else if (st == 2) {
            return in.getKeyState(InputHandler.KEY_SELECT);
        } else if (st == 3) {
            return in.getKeyState(InputHandler.KEY_START);
        } else if (st == 4) {
            return in.getKeyState(InputHandler.KEY_UP);
        } else if (st == 5) {
            return in.getKeyState(InputHandler.KEY_DOWN);
        } else if (st == 6) {
            return in.getKeyState(InputHandler.KEY_LEFT);
        } else if (st == 7) {
            return in.getKeyState(InputHandler.KEY_RIGHT);
        } else if (st == 16) {
            return (short) 0;
        } else if (st == 17) {
            return (short) 0;
        } else if (st == 18) {
            return (short) 1;
        } else if (st == 19) {
            return (short) 0;
        } else {
            return 0;
        }
    }

    public void loadROM(ROM rom) {

        if (!rom.isValid() || rom.getRomBankCount() < 1) {
            //System.out.println("NoMapper: Invalid ROM! Unable to load.");
            return;
        }

        // Load ROM into memory:
        loadPRGROM();

        // Load CHR-ROM:
        loadCHRROM();

        // Load Battery RAM (if present):
        loadBatteryRam();

        // Reset IRQ:
        //nes.getCpu().doResetInterrupt();
        nes.getCpu().requestIrq(CPU.IRQ_RESET);

    }

    protected void loadPRGROM() {

        if (rom.getRomBankCount() > 1) {
            // Load the two first banks into memory.
            loadRomBank(0, 0x8000);
            loadRomBank(1, 0xC000);
        } else {
            // Load the one bank into both memory locations:
            loadRomBank(0, 0x8000);
            loadRomBank(0, 0xC000);
        }

    }

    protected void loadCHRROM() {

        ////System.out.println("Loading CHR ROM..");

        if (rom.getVromBankCount() > 0) {
            if (rom.getVromBankCount() == 1) {
                loadVromBank(0, 0x0000);
                loadVromBank(0, 0x1000);
            } else {
                loadVromBank(0, 0x0000);
                loadVromBank(1, 0x1000);
            }
        } else {
            //System.out.println("There aren't any CHR-ROM banks..");
        }

    }

    public void loadBatteryRam() {

        if (rom.batteryRam) {

            short[] ram = rom.getBatteryRam();
            if (ram != null && ram.length == 0x2000) {

                // Load Battery RAM into memory:
                System.arraycopy(ram, 0, nes.cpuMem.mem, 0x6000, 0x2000);

            }

        }

    }

    protected void loadRomBank(int bank, int address) {

        // Loads a ROM bank into the specified address.
        bank %= rom.getRomBankCount();
        short[] data = rom.getRomBank(bank);
        //cpuMem.write(address,data,data.length);
        System.arraycopy(rom.getRomBank(bank), 0, cpuMem.mem, address, 16384);

    }

    protected void loadVromBank(int bank, int address) {

        if (rom.getVromBankCount() == 0) {
            return;
        }
        ppu.triggerRendering();

        System.arraycopy(rom.getVromBank(bank % rom.getVromBankCount()), 0, nes.ppuMem.mem, address, 4096);

        Tile[] vromTile = rom.getVromBankTiles(bank % rom.getVromBankCount());
        System.arraycopy(vromTile, 0, ppu.ptTile, address >> 4, 256);

    }

    protected void load32kRomBank(int bank, int address) {

        loadRomBank((bank * 2) % rom.getRomBankCount(), address);
        loadRomBank((bank * 2 + 1) % rom.getRomBankCount(), address + 16384);

    }

    protected void load8kVromBank(int bank4kStart, int address) {

        if (rom.getVromBankCount() == 0) {
            return;
        }
        ppu.triggerRendering();

        loadVromBank((bank4kStart) % rom.getVromBankCount(), address);
        loadVromBank((bank4kStart + 1) % rom.getVromBankCount(), address + 4096);

    }

    protected void load1kVromBank(int bank1k, int address) {

        if (rom.getVromBankCount() == 0) {
            return;
        }
        ppu.triggerRendering();

        int bank4k = (bank1k / 4) % rom.getVromBankCount();
        int bankoffset = (bank1k % 4) * 1024;
        System.arraycopy(rom.getVromBank(bank4k), 0, nes.ppuMem.mem, bankoffset, 1024);

        // Update tiles:
        Tile[] vromTile = rom.getVromBankTiles(bank4k);
        int baseIndex = address >> 4;
        for (int i = 0; i < 64; i++) {
            ppu.ptTile[baseIndex + i] = vromTile[((bank1k % 4) << 6) + i];
        }

    }

    protected void load2kVromBank(int bank2k, int address) {

        if (rom.getVromBankCount() == 0) {
            return;
        }
        ppu.triggerRendering();

        int bank4k = (bank2k / 2) % rom.getVromBankCount();
        int bankoffset = (bank2k % 2) * 2048;
        System.arraycopy(rom.getVromBank(bank4k), bankoffset, nes.ppuMem.mem, address, 2048);

        // Update tiles:
        Tile[] vromTile = rom.getVromBankTiles(bank4k);
        int baseIndex = address >> 4;
        for (int i = 0; i < 128; i++) {
            ppu.ptTile[baseIndex + i] = vromTile[((bank2k % 2) << 7) + i];
        }

    }

    protected void load8kRomBank(int bank8k, int address) {

        int bank16k = (bank8k / 2) % rom.getRomBankCount();
        int offset = (bank8k % 2) * 8192;

        short[] bank = rom.getRomBank(bank16k);
        cpuMem.write(address, bank, offset, 8192);

    }

    public void clockIrqCounter() {
        // Does nothing. This is used by the MMC3 mapper.
    }

    public void latchAccess(int address) {
        // Does nothing. This is used by MMC2.
    }

    public int syncV() {
        return 0;
    }

    public int syncH(int scanline) {
        return 0;
    }

    public void setCRC(long crc) {
    }

    public void setMouseState(boolean pressed, int x, int y) {

        mousePressed = pressed;
        mouseX = x;
        mouseY = y;

    }

    public void reset() {

        joy1StrobeState = 0;
        joy2StrobeState = 0;
        joypadLastWrite = 0;
        mousePressed = false;

    }

    public void destroy() {

        nes = null;
        cpuMem = null;
        ppuMem = null;
        rom = null;
        cpu = null;
        ppu = null;

    }
}