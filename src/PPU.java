/*
vNES
Copyright © 2006-2013 Open Emulation Project

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

public class PPU {

    private NES nes;
    private HiResTimer timer;
    private Memory ppuMem;
    private Memory sprMem;
    // Rendering Options:
    boolean showSpr0Hit = false;
    boolean showSoundBuffer = false;
    boolean clipTVcolumn = true;
    boolean clipTVrow = false;
    // Control Flags Register 1:
    public int f_nmiOnVblank;    // NMI on VBlank. 0=disable, 1=enable
    public int f_spriteSize;     // Sprite size. 0=8x8, 1=8x16
    public int f_bgPatternTable; // Background Pattern Table address. 0=0x0000,1=0x1000
    public int f_spPatternTable; // Sprite Pattern Table address. 0=0x0000,1=0x1000
    public int f_addrInc;        // PPU Address Increment. 0=1,1=32
    public int f_nTblAddress;    // Name Table Address. 0=0x2000,1=0x2400,2=0x2800,3=0x2C00
    // Control Flags Register 2:
    public int f_color;	   	 	 // Background color. 0=black, 1=blue, 2=green, 4=red
    public int f_spVisibility;   // Sprite visibility. 0=not displayed,1=displayed
    public int f_bgVisibility;   // Background visibility. 0=Not Displayed,1=displayed
    public int f_spClipping;     // Sprite clipping. 0=Sprites invisible in left 8-pixel column,1=No clipping
    public int f_bgClipping;     // Background clipping. 0=BG invisible in left 8-pixel column, 1=No clipping
    public int f_dispType;       // Display type. 0=color, 1=monochrome
    // Status flags:
    public int STATUS_VRAMWRITE = 4;
    public int STATUS_SLSPRITECOUNT = 5;
    public int STATUS_SPRITE0HIT = 6;
    public int STATUS_VBLANK = 7;
    // VRAM I/O:
    int vramAddress;
    int vramTmpAddress;
    short vramBufferedReadValue;
    boolean firstWrite = true; 		// VRAM/Scroll Hi/Lo latch
    int[] vramMirrorTable; 			// Mirroring Lookup Table.
    int i;

    // SPR-RAM I/O:
    short sramAddress; // 8-bit only.

    // Counters:
    int cntFV;
    int cntV;
    int cntH;
    int cntVT;
    int cntHT;

    // Registers:
    int regFV;
    int regV;
    int regH;
    int regVT;
    int regHT;
    int regFH;
    int regS;

    // VBlank extension for PAL emulation:
    int vblankAdd = 0;
    public int curX;
    public int scanline;
    public int lastRenderedScanline;
    public int mapperIrqCounter;
    // Sprite data:
    public int[] sprX;				// X coordinate
    public int[] sprY;				// Y coordinate
    public int[] sprTile;			// Tile Index (into pattern table)
    public int[] sprCol;			// Upper two bits of color
    public boolean[] vertFlip;		// Vertical Flip
    public boolean[] horiFlip;		// Horizontal Flip
    public boolean[] bgPriority;	// Background priority
    public int spr0HitX;	// Sprite #0 hit X coordinate
    public int spr0HitY;	// Sprite #0 hit Y coordinate
    boolean hitSpr0;

    // Tiles:
    public Tile[] ptTile;
    // Name table data:
    int[] ntable1 = new int[4];
    NameTable[] nameTable;
    int currentMirroring = -1;

    // Palette data:
    int[] sprPalette = new int[16];
    int[] imgPalette = new int[16];
    // Misc:
    boolean scanlineAlreadyRendered;
    boolean requestEndFrame;
    boolean nmiOk;
    int nmiCounter;
    short tmp;
    boolean dummyCycleToggle;

    // Vars used when updating regs/address:
    int address, b1, b2;
    // Variables used when rendering:
    int[] attrib = new int[32];
    int[] bgbuffer = new int[256 * 240];
    int[] pixrendered = new int[256 * 240];
    int[] spr0dummybuffer = new int[256 * 240];
    int[] dummyPixPriTable = new int[256 * 240];
    int[] oldFrame = new int[256 * 240];
    int[] buffer;
    int[] tpix;
    boolean[] scanlineChanged = new boolean[240];
    boolean requestRenderAll = false;
    boolean validTileData;
    int att;
    Tile[] scantile = new Tile[32];
    Tile t;
    // These are temporary variables used in rendering and sound procedures.
    // Their states outside of those procedures can be ignored.
    int curNt;
    int destIndex;
    int x, y, sx;
    int si, ei;
    int tile;
    int col;
    int baseTile;
    int tscanoffset;
    int srcy1, srcy2;
    int bufferSize, available, scale;
    public int cycles = 0;

    public PPU(NES nes) {
        this.nes = nes;
    }

    public void init() {

        // Get the memory:
        ppuMem = nes.getPpuMemory();
        sprMem = nes.getSprMemory();

        updateControlReg1(0);
        updateControlReg2(0);

        // Initialize misc vars:
        scanline = 0;
        timer = nes.getGui().getTimer();

        // Create sprite arrays:
        sprX = new int[64];
        sprY = new int[64];
        sprTile = new int[64];
        sprCol = new int[64];
        vertFlip = new boolean[64];
        horiFlip = new boolean[64];
        bgPriority = new boolean[64];

        // Create pattern table tile buffers:
        if (ptTile == null) {
            ptTile = new Tile[512];
            for (int i = 0; i < 512; i++) {
                ptTile[i] = new Tile();
            }
        }

        // Create nametable buffers:
        nameTable = new NameTable[4];
        for (int i = 0; i < 4; i++) {
            nameTable[i] = new NameTable(32, 32, "Nt" + i);
        }

        // Initialize mirroring lookup table:
        vramMirrorTable = new int[0x8000];
        for (int i = 0; i < 0x8000; i++) {
            vramMirrorTable[i] = i;
        }

        lastRenderedScanline = -1;
        curX = 0;

        // Initialize old frame buffer:
        for (int i = 0; i < oldFrame.length; i++) {
            oldFrame[i] = -1;
        }

    }


    // Sets Nametable mirroring.
    public void setMirroring(int mirroring) {

        if (mirroring == currentMirroring) {
            return;
        }

        currentMirroring = mirroring;
        triggerRendering();

        // Remove mirroring:
        if (vramMirrorTable == null) {
            vramMirrorTable = new int[0x8000];
        }
        for (int i = 0; i < 0x8000; i++) {
            vramMirrorTable[i] = i;
        }

        // Palette mirroring:
        defineMirrorRegion(0x3f20, 0x3f00, 0x20);
        defineMirrorRegion(0x3f40, 0x3f00, 0x20);
        defineMirrorRegion(0x3f80, 0x3f00, 0x20);
        defineMirrorRegion(0x3fc0, 0x3f00, 0x20);

        // Additional mirroring:
        defineMirrorRegion(0x3000, 0x2000, 0xf00);
        defineMirrorRegion(0x4000, 0x0000, 0x4000);

        if (mirroring == ROM.HORIZONTAL_MIRRORING) {


            // Horizontal mirroring.

            ntable1[0] = 0;
            ntable1[1] = 0;
            ntable1[2] = 1;
            ntable1[3] = 1;

            defineMirrorRegion(0x2400, 0x2000, 0x400);
            defineMirrorRegion(0x2c00, 0x2800, 0x400);

        } else if (mirroring == ROM.VERTICAL_MIRRORING) {

            // Vertical mirroring.

            ntable1[0] = 0;
            ntable1[1] = 1;
            ntable1[2] = 0;
            ntable1[3] = 1;

            defineMirrorRegion(0x2800, 0x2000, 0x400);
            defineMirrorRegion(0x2c00, 0x2400, 0x400);

        } else if (mirroring == ROM.SINGLESCREEN_MIRRORING) {

            // Single Screen mirroring

            ntable1[0] = 0;
            ntable1[1] = 0;
            ntable1[2] = 0;
            ntable1[3] = 0;

            defineMirrorRegion(0x2400, 0x2000, 0x400);
            defineMirrorRegion(0x2800, 0x2000, 0x400);
            defineMirrorRegion(0x2c00, 0x2000, 0x400);

        } else if (mirroring == ROM.SINGLESCREEN_MIRRORING2) {


            ntable1[0] = 1;
            ntable1[1] = 1;
            ntable1[2] = 1;
            ntable1[3] = 1;

            defineMirrorRegion(0x2400, 0x2400, 0x400);
            defineMirrorRegion(0x2800, 0x2400, 0x400);
            defineMirrorRegion(0x2c00, 0x2400, 0x400);

        } else {

            // Assume Four-screen mirroring.

            ntable1[0] = 0;
            ntable1[1] = 1;
            ntable1[2] = 2;
            ntable1[3] = 3;

        }

    }


    // Define a mirrored area in the address lookup table.
    // Assumes the regions don't overlap.
    // The 'to' region is the region that is physically in memory.
    private void defineMirrorRegion(int fromStart, int toStart, int size) {

        for (int i = 0; i < size; i++) {
            vramMirrorTable[fromStart + i] = toStart + i;
        }

    }

    // Emulates PPU cycles
    public void emulateCycles() {

        //int n = (!requestEndFrame && curX+cycles<341 && (scanline-20 < spr0HitY || scanline-22 > spr0HitY))?cycles:1;
        for (; cycles > 0; cycles--) {

            if (scanline - 21 == spr0HitY) {

                if ((curX == spr0HitX) && (f_spVisibility == 1)) {
                    // Set sprite 0 hit flag:
                    setStatusFlag(STATUS_SPRITE0HIT, true);
                }

            }

            if (requestEndFrame) {
                nmiCounter--;
                if (nmiCounter == 0) {
                    requestEndFrame = false;
                    startVBlank();
                }
            }

            curX++;
            if (curX == 341) {

                curX = 0;
                endScanline();

            }

        }

    }

    public void startVBlank() {

        // Start VBlank period:
        // Do VBlank.
        if (Globals.debug) {
            Globals.println("VBlank occurs!");
        }

        // Do NMI:
        nes.getCpu().requestIrq(CPU.IRQ_NMI);

        // Make sure everything is rendered:
        if (lastRenderedScanline < 239) {
            renderFramePartially(nes.gui.getScreenView().getBuffer(), lastRenderedScanline + 1, 240 - lastRenderedScanline);
        }

        endFrame();

        // Notify image buffer:
        nes.getGui().getScreenView().imageReady(false);

        // Reset scanline counter:
        lastRenderedScanline = -1;

        startFrame();

    }

    public void endScanline() {

        if (scanline < 19 + vblankAdd) {

            // VINT
            // do nothing.
        } else if (scanline == 19 + vblankAdd) {

            // Dummy scanline.
            // May be variable length:
            if (dummyCycleToggle) {

                // Remove dead cycle at end of scanline,
                // for next scanline:
                curX = 1;
                dummyCycleToggle = !dummyCycleToggle;

            }

        } else if (scanline == 20 + vblankAdd) {


            // Clear VBlank flag:
            setStatusFlag(STATUS_VBLANK, false);

            // Clear Sprite #0 hit flag:
            setStatusFlag(STATUS_SPRITE0HIT, false);
            hitSpr0 = false;
            spr0HitX = -1;
            spr0HitY = -1;

            if (f_bgVisibility == 1 || f_spVisibility == 1) {

                // Update counters:
                cntFV = regFV;
                cntV = regV;
                cntH = regH;
                cntVT = regVT;
                cntHT = regHT;

                if (f_bgVisibility == 1) {
                    // Render dummy scanline:
                    renderBgScanline(buffer, 0);
                }

            }

            if (f_bgVisibility == 1 && f_spVisibility == 1) {

                // Check sprite 0 hit for first scanline:
                checkSprite0(0);

            }

            if (f_bgVisibility == 1 || f_spVisibility == 1) {
                // Clock mapper IRQ Counter:
                nes.memMapper.clockIrqCounter();
            }

        } else if (scanline >= 21 + vblankAdd && scanline <= 260) {

            // Render normally:
            if (f_bgVisibility == 1) {

                if (!scanlineAlreadyRendered) {
                    // update scroll:
                    cntHT = regHT;
                    cntH = regH;
                    renderBgScanline(bgbuffer, scanline + 1 - 21);
                }
                scanlineAlreadyRendered = false;

                // Check for sprite 0 (next scanline):
                if (!hitSpr0 && f_spVisibility == 1) {
                    if (sprX[0] >= -7 && sprX[0] < 256 && sprY[0] + 1 <= (scanline - vblankAdd + 1 - 21) && (sprY[0] + 1 + (f_spriteSize == 0 ? 8 : 16)) >= (scanline - vblankAdd + 1 - 21)) {
                        if (checkSprite0(scanline + vblankAdd + 1 - 21)) {
                            ////System.out.println("found spr0. curscan="+scanline+" hitscan="+spr0HitY);
                            hitSpr0 = true;
                        }
                    }
                }

            }

            if (f_bgVisibility == 1 || f_spVisibility == 1) {
                // Clock mapper IRQ Counter:
                nes.memMapper.clockIrqCounter();
            }

        } else if (scanline == 261 + vblankAdd) {

            // Dead scanline, no rendering.
            // Set VINT:
            setStatusFlag(STATUS_VBLANK, true);
            requestEndFrame = true;
            nmiCounter = 9;

            // Wrap around:
            scanline = -1;	// will be incremented to 0

        }

        scanline++;
        regsToAddress();
        cntsToAddress();

    }

    public void startFrame() {

        int[] buffer = nes.getGui().getScreenView().getBuffer();

        // Set background color:
        int bgColor = 0;

        if (f_dispType == 0) {

            // Color display.
            // f_color determines color emphasis.
            // Use first entry of image palette as BG color.
            bgColor = imgPalette[0];

        } else {

            // Monochrome display.
            // f_color determines the bg color.
            switch (f_color) {

                case 0: {
                    // Black
                    bgColor = 0x00000;
                    break;
                }
                case 1: {
                    // Green
                    bgColor = 0x00FF00;
                }
                case 2: {
                    // Blue
                    bgColor = 0xFF0000;
                }
                case 3: {
                    // Invalid. Use black.
                    bgColor = 0x000000;
                }
                case 4: {
                    // Red
                    bgColor = 0x0000FF;
                }
                default: {
                    // Invalid. Use black.
                    bgColor = 0x0;
                }
            }

        }

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = bgColor;
        }
        for (int i = 0; i < pixrendered.length; i++) {
            pixrendered[i] = 65;
        }

    }

    public void endFrame() {

        int[] buffer = nes.getGui().getScreenView().getBuffer();

        // Draw spr#0 hit coordinates:
        if (showSpr0Hit) {
            // Spr 0 position:
            if (sprX[0] >= 0 && sprX[0] < 256 && sprY[0] >= 0 && sprY[0] < 240) {
                for (int i = 0; i < 256; i++) {
                    buffer[(sprY[0] << 8) + i] = 0xFF5555;
                }
                for (int i = 0; i < 240; i++) {
                    buffer[(i << 8) + sprX[0]] = 0xFF5555;
                }
            }
            // Hit position:
            if (spr0HitX >= 0 && spr0HitX < 256 && spr0HitY >= 0 && spr0HitY < 240) {
                for (int i = 0; i < 256; i++) {
                    buffer[(spr0HitY << 8) + i] = 0x55FF55;
                }
                for (int i = 0; i < 240; i++) {
                    buffer[(i << 8) + spr0HitX] = 0x55FF55;
                }
            }
        }

        // This is a bit lazy..
        // if either the sprites or the background should be clipped,
        // both are clipped after rendering is finished.
        if (clipTVcolumn || f_bgClipping == 0 || f_spClipping == 0) {
            // Clip left 8-pixels column:
            for (int y = 0; y < 240; y++) {
                for (int x = 0; x < 8; x++) {
                    buffer[(y << 8) + x] = 0;
                }
            }
        }

        if (clipTVcolumn) {
            // Clip right 8-pixels column too:
            for (int y = 0; y < 240; y++) {
                for (int x = 0; x < 8; x++) {
                    buffer[(y << 8) + 255 - x] = 0;
                }
            }
        }

        // Clip top and bottom 8 pixels:
        if (clipTVrow) {
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 256; x++) {
                    buffer[(y << 8) + x] = 0;
                    buffer[((239 - y) << 8) + x] = 0;
                }
            }
        }

        // Show sound buffer:
        if (showSoundBuffer && nes.getPapu().getLine() != null) {

            bufferSize = nes.getPapu().getLine().getBufferSize();
            available = nes.getPapu().getLine().available();
            scale = bufferSize / 256;

            for (int y = 0; y < 4; y++) {
                scanlineChanged[y] = true;
                for (int x = 0; x < 256; x++) {
                    if (x >= (available / scale)) {
                        buffer[y * 256 + x] = 0xFFFFFF;
                    } else {
                        buffer[y * 256 + x] = 0;
                    }
                }
            }
        }

    }

    public void updateControlReg1(int value) {

        triggerRendering();

        f_nmiOnVblank = (value >> 7) & 1;
        f_spriteSize = (value >> 5) & 1;
        f_bgPatternTable = (value >> 4) & 1;
        f_spPatternTable = (value >> 3) & 1;
        f_addrInc = (value >> 2) & 1;
        f_nTblAddress = value & 3;

        regV = (value >> 1) & 1;
        regH = value & 1;
        regS = (value >> 4) & 1;

    }

    public void updateControlReg2(int value) {

        triggerRendering();

        f_color = (value >> 5) & 7;
        f_spVisibility = (value >> 4) & 1;
        f_bgVisibility = (value >> 3) & 1;
        f_spClipping = (value >> 2) & 1;
        f_bgClipping = (value >> 1) & 1;
        f_dispType = value & 1;

        if (f_dispType == 0) {
            nes.palTable.setEmphasis(f_color);
        }
        updatePalettes();

    }

    public void setStatusFlag(int flag, boolean value) {

        int n = 1 << flag;
        int memValue = nes.getCpuMemory().load(0x2002);
        memValue = ((memValue & (255 - n)) | (value ? n : 0));
        nes.getCpuMemory().write(0x2002, (short) memValue);

    }


    // CPU Register $2002:
    // Read the Status Register.
    public short readStatusRegister() {

        tmp = nes.getCpuMemory().load(0x2002);

        // Reset scroll & VRAM Address toggle:
        firstWrite = true;

        // Clear VBlank flag:
        setStatusFlag(STATUS_VBLANK, false);

        // Fetch status data:
        return tmp;

    }


    // CPU Register $2003:
    // Write the SPR-RAM address that is used for sramWrite (Register 0x2004 in CPU memory map)
    public void writeSRAMAddress(short address) {
        sramAddress = address;
    }


    // CPU Register $2004 (R):
    // Read from SPR-RAM (Sprite RAM).
    // The address should be set first.
    public short sramLoad() {
        short tmp = sprMem.load(sramAddress);
        /*sramAddress++; // Increment address
        sramAddress%=0x100;*/
        return tmp;
    }


    // CPU Register $2004 (W):
    // Write to SPR-RAM (Sprite RAM).
    // The address should be set first.
    public void sramWrite(short value) {
        sprMem.write(sramAddress, value);
        spriteRamWriteUpdate(sramAddress, value);
        sramAddress++; // Increment address
        sramAddress %= 0x100;
    }


    // CPU Register $2005:
    // Write to scroll registers.
    // The first write is the vertical offset, the second is the
    // horizontal offset:
    public void scrollWrite(short value) {

        triggerRendering();
        if (firstWrite) {

            // First write, horizontal scroll:
            regHT = (value >> 3) & 31;
            regFH = value & 7;

        } else {

            // Second write, vertical scroll:
            regFV = value & 7;
            regVT = (value >> 3) & 31;

        }
        firstWrite = !firstWrite;

    }

    // CPU Register $2006:
    // Sets the adress used when reading/writing from/to VRAM.
    // The first write sets the high byte, the second the low byte.
    public void writeVRAMAddress(int address) {

        if (firstWrite) {

            regFV = (address >> 4) & 3;
            regV = (address >> 3) & 1;
            regH = (address >> 2) & 1;
            regVT = (regVT & 7) | ((address & 3) << 3);

        } else {

            triggerRendering();

            regVT = (regVT & 24) | ((address >> 5) & 7);
            regHT = address & 31;

            cntFV = regFV;
            cntV = regV;
            cntH = regH;
            cntVT = regVT;
            cntHT = regHT;

            checkSprite0(scanline - vblankAdd + 1 - 21);

        }

        firstWrite = !firstWrite;

        // Invoke mapper latch:
        cntsToAddress();
        if (vramAddress < 0x2000) {
            nes.memMapper.latchAccess(vramAddress);
        }

    }

    // CPU Register $2007(R):
    // Read from PPU memory. The address should be set first.
    public short vramLoad() {

        cntsToAddress();
        regsToAddress();

        // If address is in range 0x0000-0x3EFF, return buffered values:
        if (vramAddress <= 0x3EFF) {

            short tmp = vramBufferedReadValue;

            // Update buffered value:
            if (vramAddress < 0x2000) {
                vramBufferedReadValue = ppuMem.load(vramAddress);
            } else {
                vramBufferedReadValue = mirroredLoad(vramAddress);
            }

            // Mapper latch access:
            if (vramAddress < 0x2000) {
                nes.memMapper.latchAccess(vramAddress);
            }

            // Increment by either 1 or 32, depending on d2 of Control Register 1:
            vramAddress += (f_addrInc == 1 ? 32 : 1);

            cntsFromAddress();
            regsFromAddress();
            return tmp; // Return the previous buffered value.

        }

        // No buffering in this mem range. Read normally.
        short tmp = mirroredLoad(vramAddress);

        // Increment by either 1 or 32, depending on d2 of Control Register 1:
        vramAddress += (f_addrInc == 1 ? 32 : 1);

        cntsFromAddress();
        regsFromAddress();

        return tmp;

    }

    // CPU Register $2007(W):
    // Write to PPU memory. The address should be set first.
    public void vramWrite(short value) {

        triggerRendering();
        cntsToAddress();
        regsToAddress();

        if (vramAddress >= 0x2000) {
            // Mirroring is used.
            mirroredWrite(vramAddress, value);
        } else {

            // Write normally.
            writeMem(vramAddress, value);

            // Invoke mapper latch:
            nes.memMapper.latchAccess(vramAddress);

        }

        // Increment by either 1 or 32, depending on d2 of Control Register 1:
        vramAddress += (f_addrInc == 1 ? 32 : 1);
        regsFromAddress();
        cntsFromAddress();

    }

    // CPU Register $4014:
    // Write 256 bytes of main memory
    // into Sprite RAM.
    public void sramDMA(short value) {

        Memory cpuMem = nes.getCpuMemory();
        int baseAddress = value * 0x100;
        short data;
        for (int i = sramAddress; i < 256; i++) {
            data = cpuMem.load(baseAddress + i);
            sprMem.write(i, data);
            spriteRamWriteUpdate(i, data);
        }

        nes.getCpu().haltCycles(513);

    }

    // Updates the scroll registers from a new VRAM address.
    private void regsFromAddress() {

        address = (vramTmpAddress >> 8) & 0xFF;
        regFV = (address >> 4) & 7;
        regV = (address >> 3) & 1;
        regH = (address >> 2) & 1;
        regVT = (regVT & 7) | ((address & 3) << 3);

        address = vramTmpAddress & 0xFF;
        regVT = (regVT & 24) | ((address >> 5) & 7);
        regHT = address & 31;



    }

    // Updates the scroll registers from a new VRAM address.
    private void cntsFromAddress() {

        address = (vramAddress >> 8) & 0xFF;
        cntFV = (address >> 4) & 3;
        cntV = (address >> 3) & 1;
        cntH = (address >> 2) & 1;
        cntVT = (cntVT & 7) | ((address & 3) << 3);

        address = vramAddress & 0xFF;
        cntVT = (cntVT & 24) | ((address >> 5) & 7);
        cntHT = address & 31;

    }

    private void regsToAddress() {

        b1 = (regFV & 7) << 4;
        b1 |= (regV & 1) << 3;
        b1 |= (regH & 1) << 2;
        b1 |= (regVT >> 3) & 3;

        b2 = (regVT & 7) << 5;
        b2 |= regHT & 31;

        vramTmpAddress = ((b1 << 8) | b2) & 0x7FFF;

    }

    private void cntsToAddress() {

        b1 = (cntFV & 7) << 4;
        b1 |= (cntV & 1) << 3;
        b1 |= (cntH & 1) << 2;
        b1 |= (cntVT >> 3) & 3;

        b2 = (cntVT & 7) << 5;
        b2 |= cntHT & 31;

        vramAddress = ((b1 << 8) | b2) & 0x7FFF;

    }

    private void incTileCounter(int count) {

        for (i = count; i != 0; i--) {
            cntHT++;
            if (cntHT == 32) {
                cntHT = 0;
                cntVT++;
                if (cntVT >= 30) {
                    cntH++;
                    if (cntH == 2) {
                        cntH = 0;
                        cntV++;
                        if (cntV == 2) {
                            cntV = 0;
                            cntFV++;
                            cntFV &= 0x7;
                        }
                    }
                }
            }
        }

    }

    // Reads from memory, taking into account
    // mirroring/mapping of address ranges.
    private short mirroredLoad(int address) {

        return ppuMem.load(vramMirrorTable[address]);

    }

    // Writes to memory, taking into account
    // mirroring/mapping of address ranges.
    private void mirroredWrite(int address, short value) {

        if (address >= 0x3f00 && address < 0x3f20) {

            // Palette write mirroring.

            if (address == 0x3F00 || address == 0x3F10) {

                writeMem(0x3F00, value);
                writeMem(0x3F10, value);

            } else if (address == 0x3F04 || address == 0x3F14) {

                writeMem(0x3F04, value);
                writeMem(0x3F14, value);

            } else if (address == 0x3F08 || address == 0x3F18) {

                writeMem(0x3F08, value);
                writeMem(0x3F18, value);

            } else if (address == 0x3F0C || address == 0x3F1C) {

                writeMem(0x3F0C, value);
                writeMem(0x3F1C, value);

            } else {

                writeMem(address, value);

            }

        } else {

            // Use lookup table for mirrored address:
            if (address < vramMirrorTable.length) {
                writeMem(vramMirrorTable[address], value);
            } else {
                if (Globals.debug) {
                    //System.out.println("Invalid VRAM address: "+Misc.hex16(address));
                    nes.getCpu().setCrashed(true);
                }
            }

        }

    }

    public void triggerRendering() {

        if (scanline - vblankAdd >= 21 && scanline - vblankAdd <= 260) {

            // Render sprites, and combine:
            renderFramePartially(buffer, lastRenderedScanline + 1, scanline - vblankAdd - 21 - lastRenderedScanline);

            // Set last rendered scanline:
            lastRenderedScanline = scanline - vblankAdd - 21;

        }

    }

    private void renderFramePartially(int[] buffer, int startScan, int scanCount) {

        if (f_spVisibility == 1 && !Globals.disableSprites) {
            renderSpritesPartially(startScan, scanCount, true);
        }

        if (f_bgVisibility == 1) {
            si = startScan << 8;
            ei = (startScan + scanCount) << 8;
            if (ei > 0xF000) {
                ei = 0xF000;
            }
            for (destIndex = si; destIndex < ei; destIndex++) {
                if (pixrendered[destIndex] > 0xFF) {
                    buffer[destIndex] = bgbuffer[destIndex];
                }
            }
        }

        if (f_spVisibility == 1 && !Globals.disableSprites) {
            renderSpritesPartially(startScan, scanCount, false);
        }

        BufferView screen = nes.getGui().getScreenView();
        if (screen.scalingEnabled() && !screen.useHWScaling() && !requestRenderAll) {

            // Check which scanlines have changed, to try to
            // speed up scaling:
            int j, jmax;
            if (startScan + scanCount > 240) {
                scanCount = 240 - startScan;
            }
            for (int i = startScan; i < startScan + scanCount; i++) {
                scanlineChanged[i] = false;
                si = i << 8;
                jmax = si + 256;
                for (j = si; j < jmax; j++) {
                    if (buffer[j] != oldFrame[j]) {
                        scanlineChanged[i] = true;
                        break;
                    }
                    oldFrame[j] = buffer[j];
                }
                System.arraycopy(buffer, j, oldFrame, j, jmax - j);
            }

        }

        validTileData = false;

    }

    private void renderBgScanline(int[] buffer, int scan) {

        baseTile = (regS == 0 ? 0 : 256);
        destIndex = (scan << 8) - regFH;
        curNt = ntable1[cntV + cntV + cntH];

        cntHT = regHT;
        cntH = regH;
        curNt = ntable1[cntV + cntV + cntH];

        if (scan < 240 && (scan - cntFV) >= 0) {

            tscanoffset = cntFV << 3;
            y = scan - cntFV;
            for (tile = 0; tile < 32; tile++) {

                if (scan >= 0) {

                    // Fetch tile & attrib data:
                    if (validTileData) {
                        // Get data from array:
                        t = scantile[tile];
                        tpix = t.pix;
                        att = attrib[tile];
                    } else {
                        // Fetch data:
                        t = ptTile[baseTile + nameTable[curNt].getTileIndex(cntHT, cntVT)];
                        tpix = t.pix;
                        att = nameTable[curNt].getAttrib(cntHT, cntVT);
                        scantile[tile] = t;
                        attrib[tile] = att;
                    }

                    // Render tile scanline:
                    sx = 0;
                    x = (tile << 3) - regFH;
                    if (x > -8) {
                        if (x < 0) {
                            destIndex -= x;
                            sx = -x;
                        }
                        if (t.opaque[cntFV]) {
                            for (; sx < 8; sx++) {
                                buffer[destIndex] = imgPalette[tpix[tscanoffset + sx] + att];
                                pixrendered[destIndex] |= 256;
                                destIndex++;
                            }
                        } else {
                            for (; sx < 8; sx++) {
                                col = tpix[tscanoffset + sx];
                                if (col != 0) {
                                    buffer[destIndex] = imgPalette[col + att];
                                    pixrendered[destIndex] |= 256;
                                }
                                destIndex++;
                            }
                        }
                    }

                }

                // Increase Horizontal Tile Counter:
                cntHT++;
                if (cntHT == 32) {
                    cntHT = 0;
                    cntH++;
                    cntH %= 2;
                    curNt = ntable1[(cntV << 1) + cntH];
                }


            }

            // Tile data for one row should now have been fetched,
            // so the data in the array is valid.
            validTileData = true;

        }

        // update vertical scroll:
        cntFV++;
        if (cntFV == 8) {
            cntFV = 0;
            cntVT++;
            if (cntVT == 30) {
                cntVT = 0;
                cntV++;
                cntV %= 2;
                curNt = ntable1[(cntV << 1) + cntH];
            } else if (cntVT == 32) {
                cntVT = 0;
            }

            // Invalidate fetched data:
            validTileData = false;

        }

    }

    private void renderSpritesPartially(int startscan, int scancount, boolean bgPri) {

        buffer = nes.getGui().getScreenView().getBuffer();
        if (f_spVisibility == 1) {

            int sprT1, sprT2;

            for (int i = 0; i < 64; i++) {
                if (bgPriority[i] == bgPri && sprX[i] >= 0 && sprX[i] < 256 && sprY[i] + 8 >= startscan && sprY[i] < startscan + scancount) {
                    // Show sprite.
                    if (f_spriteSize == 0) {
                        // 8x8 sprites

                        srcy1 = 0;
                        srcy2 = 8;

                        if (sprY[i] < startscan) {
                            srcy1 = startscan - sprY[i] - 1;
                        }

                        if (sprY[i] + 8 > startscan + scancount) {
                            srcy2 = startscan + scancount - sprY[i] + 1;
                        }

                        if (f_spPatternTable == 0) {
                            ptTile[sprTile[i]].render(0, srcy1, 8, srcy2, sprX[i], sprY[i] + 1, buffer, sprCol[i], sprPalette, horiFlip[i], vertFlip[i], i, pixrendered);
                        } else {
                            ptTile[sprTile[i] + 256].render(0, srcy1, 8, srcy2, sprX[i], sprY[i] + 1, buffer, sprCol[i], sprPalette, horiFlip[i], vertFlip[i], i, pixrendered);
                        }
                    } else {
                        // 8x16 sprites
                        int top = sprTile[i];
                        if ((top & 1) != 0) {
                            top = sprTile[i] - 1 + 256;
                        }

                        srcy1 = 0;
                        srcy2 = 8;

                        if (sprY[i] < startscan) {
                            srcy1 = startscan - sprY[i] - 1;
                        }

                        if (sprY[i] + 8 > startscan + scancount) {
                            srcy2 = startscan + scancount - sprY[i];
                        }

                        ptTile[top + (vertFlip[i] ? 1 : 0)].render(0, srcy1, 8, srcy2, sprX[i], sprY[i] + 1, buffer, sprCol[i], sprPalette, horiFlip[i], vertFlip[i], i, pixrendered);

                        srcy1 = 0;
                        srcy2 = 8;

                        if (sprY[i] + 8 < startscan) {
                            srcy1 = startscan - (sprY[i] + 8 + 1);
                        }

                        if (sprY[i] + 16 > startscan + scancount) {
                            srcy2 = startscan + scancount - (sprY[i] + 8);
                        }

                        ptTile[top + (vertFlip[i] ? 0 : 1)].render(0, srcy1, 8, srcy2, sprX[i], sprY[i] + 1 + 8, buffer, sprCol[i], sprPalette, horiFlip[i], vertFlip[i], i, pixrendered);

                    }
                }
            }
        }

    }

    private boolean checkSprite0(int scan) {

        spr0HitX = -1;
        spr0HitY = -1;

        int toffset;
        int tIndexAdd = (f_spPatternTable == 0 ? 0 : 256);
        int x, y;
        int bufferIndex;
        int col;
        boolean bgPri;
        Tile t;

        x = sprX[0];
        y = sprY[0] + 1;


        if (f_spriteSize == 0) {

            // 8x8 sprites.

            // Check range:
            if (y <= scan && y + 8 > scan && x >= -7 && x < 256) {

                // Sprite is in range.
                // Draw scanline:
                t = ptTile[sprTile[0] + tIndexAdd];
                col = sprCol[0];
                bgPri = bgPriority[0];

                if (vertFlip[0]) {
                    toffset = 7 - (scan - y);
                } else {
                    toffset = scan - y;
                }
                toffset *= 8;

                bufferIndex = scan * 256 + x;
                if (horiFlip[0]) {
                    for (int i = 7; i >= 0; i--) {
                        if (x >= 0 && x < 256) {
                            if (bufferIndex >= 0 && bufferIndex < 61440 && pixrendered[bufferIndex] != 0) {
                                if (t.pix[toffset + i] != 0) {
                                    spr0HitX = bufferIndex % 256;
                                    spr0HitY = scan;
                                    return true;
                                }
                            }
                        }
                        x++;
                        bufferIndex++;
                    }

                } else {

                    for (int i = 0; i < 8; i++) {
                        if (x >= 0 && x < 256) {
                            if (bufferIndex >= 0 && bufferIndex < 61440 && pixrendered[bufferIndex] != 0) {
                                if (t.pix[toffset + i] != 0) {
                                    spr0HitX = bufferIndex % 256;
                                    spr0HitY = scan;
                                    return true;
                                }
                            }
                        }
                        x++;
                        bufferIndex++;
                    }

                }

            }


        } else {

            // 8x16 sprites:

            // Check range:
            if (y <= scan && y + 16 > scan && x >= -7 && x < 256) {

                // Sprite is in range.
                // Draw scanline:

                if (vertFlip[0]) {
                    toffset = 15 - (scan - y);
                } else {
                    toffset = scan - y;
                }

                if (toffset < 8) {
                    // first half of sprite.
                    t = ptTile[sprTile[0] + (vertFlip[0] ? 1 : 0) + ((sprTile[0] & 1) != 0 ? 255 : 0)];
                } else {
                    // second half of sprite.
                    t = ptTile[sprTile[0] + (vertFlip[0] ? 0 : 1) + ((sprTile[0] & 1) != 0 ? 255 : 0)];
                    if (vertFlip[0]) {
                        toffset = 15 - toffset;
                    } else {
                        toffset -= 8;
                    }
                }
                toffset *= 8;
                col = sprCol[0];
                bgPri = bgPriority[0];

                bufferIndex = scan * 256 + x;
                if (horiFlip[0]) {

                    for (int i = 7; i >= 0; i--) {
                        if (x >= 0 && x < 256) {
                            if (bufferIndex >= 0 && bufferIndex < 61440 && pixrendered[bufferIndex] != 0) {
                                if (t.pix[toffset + i] != 0) {
                                    spr0HitX = bufferIndex % 256;
                                    spr0HitY = scan;
                                    return true;
                                }
                            }
                        }
                        x++;
                        bufferIndex++;
                    }

                } else {

                    for (int i = 0; i < 8; i++) {
                        if (x >= 0 && x < 256) {
                            if (bufferIndex >= 0 && bufferIndex < 61440 && pixrendered[bufferIndex] != 0) {
                                if (t.pix[toffset + i] != 0) {
                                    spr0HitX = bufferIndex % 256;
                                    spr0HitY = scan;
                                    return true;
                                }
                            }
                        }
                        x++;
                        bufferIndex++;
                    }

                }

            }

        }

        return false;

    }

    // Renders the contents of the
    // pattern table into an image.
    public void renderPattern() {

        BufferView scr = nes.getGui().getPatternView();
        int[] buffer = scr.getBuffer();

        int tIndex = 0;
        for (int j = 0; j < 2; j++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    ptTile[tIndex].renderSimple(j * 128 + x * 8, y * 8, buffer, 0, sprPalette);
                    tIndex++;
                }
            }
        }
        nes.getGui().getPatternView().imageReady(false);

    }

    public void renderNameTables() {

        int[] buffer = nes.getGui().getNameTableView().getBuffer();
        if (f_bgPatternTable == 0) {
            baseTile = 0;
        } else {
            baseTile = 256;
        }

        int ntx_max = 2;
        int nty_max = 2;

        if (currentMirroring == ROM.HORIZONTAL_MIRRORING) {
            ntx_max = 1;
        } else if (currentMirroring == ROM.VERTICAL_MIRRORING) {
            nty_max = 1;
        }

        for (int nty = 0; nty < nty_max; nty++) {
            for (int ntx = 0; ntx < ntx_max; ntx++) {

                int nt = ntable1[nty * 2 + ntx];
                int x = ntx * 128;
                int y = nty * 120;

                // Render nametable:
                for (int ty = 0; ty < 30; ty++) {
                    for (int tx = 0; tx < 32; tx++) {
                        //ptTile[baseTile+nameTable[nt].getTileIndex(tx,ty)].render(0,0,4,4,x+tx*4,y+ty*4,buffer,nameTable[nt].getAttrib(tx,ty),imgPalette,false,false,0,dummyPixPriTable);
                        ptTile[baseTile + nameTable[nt].getTileIndex(tx, ty)].renderSmall(x + tx * 4, y + ty * 4, buffer, nameTable[nt].getAttrib(tx, ty), imgPalette);
                    }
                }

            }
        }

        if (currentMirroring == ROM.HORIZONTAL_MIRRORING) {
            // double horizontally:
            for (int y = 0; y < 240; y++) {
                for (int x = 0; x < 128; x++) {
                    buffer[(y << 8) + 128 + x] = buffer[(y << 8) + x];
                }
            }
        } else if (currentMirroring == ROM.VERTICAL_MIRRORING) {
            // double vertically:
            for (int y = 0; y < 120; y++) {
                for (int x = 0; x < 256; x++) {
                    buffer[(y << 8) + 0x7800 + x] = buffer[(y << 8) + x];
                }
            }
        }

        nes.getGui().getNameTableView().imageReady(false);

    }

    private void renderPalettes() {

        int[] buffer = nes.getGui().getImgPalView().getBuffer();
        for (int i = 0; i < 16; i++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    buffer[y * 256 + i * 16 + x] = imgPalette[i];
                }
            }
        }

        buffer = nes.getGui().getSprPalView().getBuffer();
        for (int i = 0; i < 16; i++) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    buffer[y * 256 + i * 16 + x] = sprPalette[i];
                }
            }
        }

        nes.getGui().getImgPalView().imageReady(false);
        nes.getGui().getSprPalView().imageReady(false);

    }


    // This will write to PPU memory, and
    // update internally buffered data
    // appropriately.
    private void writeMem(int address, short value) {

        ppuMem.write(address, value);

        // Update internally buffered data:
        if (address < 0x2000) {

            ppuMem.write(address, value);
            patternWrite(address, value);

        } else if (address >= 0x2000 && address < 0x23c0) {

            nameTableWrite(ntable1[0], address - 0x2000, value);

        } else if (address >= 0x23c0 && address < 0x2400) {

            attribTableWrite(ntable1[0], address - 0x23c0, value);

        } else if (address >= 0x2400 && address < 0x27c0) {

            nameTableWrite(ntable1[1], address - 0x2400, value);

        } else if (address >= 0x27c0 && address < 0x2800) {

            attribTableWrite(ntable1[1], address - 0x27c0, value);

        } else if (address >= 0x2800 && address < 0x2bc0) {

            nameTableWrite(ntable1[2], address - 0x2800, value);

        } else if (address >= 0x2bc0 && address < 0x2c00) {

            attribTableWrite(ntable1[2], address - 0x2bc0, value);

        } else if (address >= 0x2c00 && address < 0x2fc0) {

            nameTableWrite(ntable1[3], address - 0x2c00, value);

        } else if (address >= 0x2fc0 && address < 0x3000) {

            attribTableWrite(ntable1[3], address - 0x2fc0, value);

        } else if (address >= 0x3f00 && address < 0x3f20) {

            updatePalettes();

        }

    }

    // Reads data from $3f00 to $f20
    // into the two buffered palettes.
    public void updatePalettes() {

        for (int i = 0; i < 16; i++) {
            if (f_dispType == 0) {
                imgPalette[i] = nes.palTable.getEntry(ppuMem.load(0x3f00 + i) & 63);
            } else {
                imgPalette[i] = nes.palTable.getEntry(ppuMem.load(0x3f00 + i) & 32);
            }
        }
        for (int i = 0; i < 16; i++) {
            if (f_dispType == 0) {
                sprPalette[i] = nes.palTable.getEntry(ppuMem.load(0x3f10 + i) & 63);
            } else {
                sprPalette[i] = nes.palTable.getEntry(ppuMem.load(0x3f10 + i) & 32);
            }
        }

    //renderPalettes();

    }


    // Updates the internal pattern
    // table buffers with this new byte.
    public void patternWrite(int address, short value) {
        int tileIndex = address / 16;
        int leftOver = address % 16;
        if (leftOver < 8) {
            ptTile[tileIndex].setScanline(leftOver, value, ppuMem.load(address + 8));
        } else {
            ptTile[tileIndex].setScanline(leftOver - 8, ppuMem.load(address - 8), value);
        }
    }

    public void patternWrite(int address, short[] value, int offset, int length) {

        int tileIndex;
        int leftOver;

        for (int i = 0; i < length; i++) {

            tileIndex = (address + i) >> 4;
            leftOver = (address + i) % 16;

            if (leftOver < 8) {
                ptTile[tileIndex].setScanline(leftOver, value[offset + i], ppuMem.load(address + 8 + i));
            } else {
                ptTile[tileIndex].setScanline(leftOver - 8, ppuMem.load(address - 8 + i), value[offset + i]);
            }

        }

    }

    public void invalidateFrameCache() {

        // Clear the no-update scanline buffer:
        for (int i = 0; i < 240; i++) {
            scanlineChanged[i] = true;
        }
        java.util.Arrays.fill(oldFrame, -1);
        requestRenderAll = true;

    }

    // Updates the internal name table buffers
    // with this new byte.
    public void nameTableWrite(int index, int address, short value) {
        nameTable[index].writeTileIndex(address, value);

        // Update Sprite #0 hit:
        //updateSpr0Hit();
        checkSprite0(scanline + 1 - vblankAdd - 21);

    }

    // Updates the internal pattern
    // table buffers with this new attribute
    // table byte.
    public void attribTableWrite(int index, int address, short value) {
        nameTable[index].writeAttrib(address, value);
    }

    // Updates the internally buffered sprite
    // data with this new byte of info.
    public void spriteRamWriteUpdate(int address, short value) {

        int tIndex = address / 4;

        if (tIndex == 0) {
            //updateSpr0Hit();
            checkSprite0(scanline + 1 - vblankAdd - 21);
        }

        if (address % 4 == 0) {

            // Y coordinate
            sprY[tIndex] = value;

        } else if (address % 4 == 1) {

            // Tile index
            sprTile[tIndex] = value;

        } else if (address % 4 == 2) {

            // Attributes
            vertFlip[tIndex] = ((value & 0x80) != 0);
            horiFlip[tIndex] = ((value & 0x40) != 0);
            bgPriority[tIndex] = ((value & 0x20) != 0);
            sprCol[tIndex] = (value & 3) << 2;

        } else if (address % 4 == 3) {

            // X coordinate
            sprX[tIndex] = value;

        }

    }

    public void doNMI() {

        // Set VBlank flag:
        setStatusFlag(STATUS_VBLANK, true);
        //nes.getCpu().doNonMaskableInterrupt();
        nes.getCpu().requestIrq(CPU.IRQ_NMI);

    }

    public int statusRegsToInt() {

        int ret = 0;
        ret = (f_nmiOnVblank) |
                (f_spriteSize << 1) |
                (f_bgPatternTable << 2) |
                (f_spPatternTable << 3) |
                (f_addrInc << 4) |
                (f_nTblAddress << 5) |
                (f_color << 6) |
                (f_spVisibility << 7) |
                (f_bgVisibility << 8) |
                (f_spClipping << 9) |
                (f_bgClipping << 10) |
                (f_dispType << 11);

        return ret;

    }

    public void statusRegsFromInt(int n) {

        f_nmiOnVblank = (n) & 0x1;
        f_spriteSize = (n >> 1) & 0x1;
        f_bgPatternTable = (n >> 2) & 0x1;
        f_spPatternTable = (n >> 3) & 0x1;
        f_addrInc = (n >> 4) & 0x1;
        f_nTblAddress = (n >> 5) & 0x1;

        f_color = (n >> 6) & 0x1;
        f_spVisibility = (n >> 7) & 0x1;
        f_bgVisibility = (n >> 8) & 0x1;
        f_spClipping = (n >> 9) & 0x1;
        f_bgClipping = (n >> 10) & 0x1;
        f_dispType = (n >> 11) & 0x1;

    }

    public void stateLoad(ByteBuffer buf) {

        // Check version:
        if (buf.readByte() == 1) {

            // Counters:
            cntFV = buf.readInt();
            cntV = buf.readInt();
            cntH = buf.readInt();
            cntVT = buf.readInt();
            cntHT = buf.readInt();


            // Registers:
            regFV = buf.readInt();
            regV = buf.readInt();
            regH = buf.readInt();
            regVT = buf.readInt();
            regHT = buf.readInt();
            regFH = buf.readInt();
            regS = buf.readInt();


            // VRAM address:
            vramAddress = buf.readInt();
            vramTmpAddress = buf.readInt();


            // Control/Status registers:
            statusRegsFromInt(buf.readInt());


            // VRAM I/O:
            vramBufferedReadValue = (short) buf.readInt();
            firstWrite = buf.readBoolean();
            //System.out.println("firstWrite: "+firstWrite);


            // Mirroring:
            //currentMirroring = -1;
            //setMirroring(buf.readInt());
            for (int i = 0; i < vramMirrorTable.length; i++) {
                vramMirrorTable[i] = buf.readInt();
            }


            // SPR-RAM I/O:
            sramAddress = (short) buf.readInt();

            // Rendering progression:
            curX = buf.readInt();
            scanline = buf.readInt();
            lastRenderedScanline = buf.readInt();


            // Misc:
            requestEndFrame = buf.readBoolean();
            nmiOk = buf.readBoolean();
            dummyCycleToggle = buf.readBoolean();
            nmiCounter = buf.readInt();
            tmp = (short) buf.readInt();


            // Stuff used during rendering:
            for (int i = 0; i < bgbuffer.length; i++) {
                bgbuffer[i] = buf.readByte();
            }
            for (int i = 0; i < pixrendered.length; i++) {
                pixrendered[i] = buf.readByte();
            }

            // Name tables:
            for (int i = 0; i < 4; i++) {
                ntable1[i] = buf.readByte();
                nameTable[i].stateLoad(buf);
            }

            // Pattern data:
            for (int i = 0; i < ptTile.length; i++) {
                ptTile[i].stateLoad(buf);
            }

            // Update internally stored stuff from VRAM memory:
			/*short[] mem = ppuMem.mem;

            // Palettes:
            for(int i=0x3f00;i<0x3f20;i++){
            writeMem(i,mem[i]);
            }
             */
            // Sprite data:
            short[] sprmem = nes.getSprMemory().mem;
            for (int i = 0; i < sprmem.length; i++) {
                spriteRamWriteUpdate(i, sprmem[i]);
            }

        }

    }

    public void stateSave(ByteBuffer buf) {


        // Version:
        buf.putByte((short) 1);


        // Counters:
        buf.putInt(cntFV);
        buf.putInt(cntV);
        buf.putInt(cntH);
        buf.putInt(cntVT);
        buf.putInt(cntHT);


        // Registers:
        buf.putInt(regFV);
        buf.putInt(regV);
        buf.putInt(regH);
        buf.putInt(regVT);
        buf.putInt(regHT);
        buf.putInt(regFH);
        buf.putInt(regS);


        // VRAM address:
        buf.putInt(vramAddress);
        buf.putInt(vramTmpAddress);


        // Control/Status registers:
        buf.putInt(statusRegsToInt());


        // VRAM I/O:
        buf.putInt(vramBufferedReadValue);
        //System.out.println("firstWrite: "+firstWrite);
        buf.putBoolean(firstWrite);

        // Mirroring:
        //buf.putInt(currentMirroring);
        for (int i = 0; i < vramMirrorTable.length; i++) {
            buf.putInt(vramMirrorTable[i]);
        }


        // SPR-RAM I/O:
        buf.putInt(sramAddress);


        // Rendering progression:
        buf.putInt(curX);
        buf.putInt(scanline);
        buf.putInt(lastRenderedScanline);


        // Misc:
        buf.putBoolean(requestEndFrame);
        buf.putBoolean(nmiOk);
        buf.putBoolean(dummyCycleToggle);
        buf.putInt(nmiCounter);
        buf.putInt(tmp);


        // Stuff used during rendering:
        for (int i = 0; i < bgbuffer.length; i++) {
            buf.putByte((short) bgbuffer[i]);
        }
        for (int i = 0; i < pixrendered.length; i++) {
            buf.putByte((short) pixrendered[i]);
        }

        // Name tables:
        for (int i = 0; i < 4; i++) {
            buf.putByte((short) ntable1[i]);
            nameTable[i].stateSave(buf);
        }

        // Pattern data:
        for (int i = 0; i < ptTile.length; i++) {
            ptTile[i].stateSave(buf);
        }

    }

    // Reset PPU:
    public void reset() {

        ppuMem.reset();
        sprMem.reset();

        vramBufferedReadValue = 0;
        sramAddress = 0;
        curX = 0;
        scanline = 0;
        lastRenderedScanline = 0;
        spr0HitX = 0;
        spr0HitY = 0;
        mapperIrqCounter = 0;

        currentMirroring = -1;

        firstWrite = true;
        requestEndFrame = false;
        nmiOk = false;
        hitSpr0 = false;
        dummyCycleToggle = false;
        validTileData = false;
        nmiCounter = 0;
        tmp = 0;
        att = 0;
        i = 0;

        // Control Flags Register 1:
        f_nmiOnVblank = 0;    // NMI on VBlank. 0=disable, 1=enable
        f_spriteSize = 0;     // Sprite size. 0=8x8, 1=8x16
        f_bgPatternTable = 0; // Background Pattern Table address. 0=0x0000,1=0x1000
        f_spPatternTable = 0; // Sprite Pattern Table address. 0=0x0000,1=0x1000
        f_addrInc = 0;        // PPU Address Increment. 0=1,1=32
        f_nTblAddress = 0;    // Name Table Address. 0=0x2000,1=0x2400,2=0x2800,3=0x2C00

        // Control Flags Register 2:
        f_color = 0;	   	  // Background color. 0=black, 1=blue, 2=green, 4=red
        f_spVisibility = 0;   // Sprite visibility. 0=not displayed,1=displayed
        f_bgVisibility = 0;   // Background visibility. 0=Not Displayed,1=displayed
        f_spClipping = 0;     // Sprite clipping. 0=Sprites invisible in left 8-pixel column,1=No clipping
        f_bgClipping = 0;     // Background clipping. 0=BG invisible in left 8-pixel column, 1=No clipping
        f_dispType = 0;       // Display type. 0=color, 1=monochrome


        // Counters:
        cntFV = 0;
        cntV = 0;
        cntH = 0;
        cntVT = 0;
        cntHT = 0;

        // Registers:
        regFV = 0;
        regV = 0;
        regH = 0;
        regVT = 0;
        regHT = 0;
        regFH = 0;
        regS = 0;

        java.util.Arrays.fill(scanlineChanged, true);
        java.util.Arrays.fill(oldFrame, -1);

        // Initialize stuff:
        init();

    }

    public void destroy() {

        nes = null;
        ppuMem = null;
        sprMem = null;
        scantile = null;

    }
}