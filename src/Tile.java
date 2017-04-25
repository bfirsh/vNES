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

import java.io.*;

public class Tile {

    // Tile data:
    int[] pix;
    int fbIndex;
    int tIndex;
    int x, y;
    int w, h;
    int incX, incY;
    int palIndex;
    int tpri;
    int c;
    public boolean initialized = false;
    public boolean[] opaque = new boolean[8];

    public Tile() {
        pix = new int[64];
    }

    public void setBuffer(short[] scanline) {
        for (y = 0; y < 8; y++) {
            setScanline(y, scanline[y], scanline[y + 8]);
        }
    }

    public void setScanline(int sline, short b1, short b2) {
        initialized = true;
        tIndex = sline << 3;
        for (x = 0; x < 8; x++) {
            pix[tIndex + x] = ((b1 >> (7 - x)) & 1) + (((b2 >> (7 - x)) & 1) << 1);
            if (pix[tIndex + x] == 0) {
                opaque[sline] = false;
            }
        }
    }

    public void renderSimple(int dx, int dy, int[] fBuffer, int palAdd, int[] palette) {

        tIndex = 0;
        fbIndex = (dy << 8) + dx;
        for (y = 8; y != 0; y--) {
            for (x = 8; x != 0; x--) {
                palIndex = pix[tIndex];
                if (palIndex != 0) {
                    fBuffer[fbIndex] = palette[palIndex + palAdd];
                }
                fbIndex++;
                tIndex++;
            }
            fbIndex -= 8;
            fbIndex += 256;
        }

    }

    public void renderSmall(int dx, int dy, int[] buffer, int palAdd, int[] palette) {

        tIndex = 0;
        fbIndex = (dy << 8) + dx;
        for (y = 0; y < 4; y++) {
            for (x = 0; x < 4; x++) {

                c = (palette[pix[tIndex] + palAdd] >> 2) & 0x003F3F3F;
                c += (palette[pix[tIndex + 1] + palAdd] >> 2) & 0x003F3F3F;
                c += (palette[pix[tIndex + 8] + palAdd] >> 2) & 0x003F3F3F;
                c += (palette[pix[tIndex + 9] + palAdd] >> 2) & 0x003F3F3F;
                buffer[fbIndex] = c;
                fbIndex++;
                tIndex += 2;
            }
            tIndex += 8;
            fbIndex += 252;
        }

    }

    public void render(int srcx1, int srcy1, int srcx2, int srcy2, int dx, int dy, int[] fBuffer, int palAdd, int[] palette, boolean flipHorizontal, boolean flipVertical, int pri, int[] priTable) {

        if (dx < -7 || dx >= 256 || dy < -7 || dy >= 240) {
            return;
        }

        w = srcx2 - srcx1;
        h = srcy2 - srcy1;

        if (dx < 0) {
            srcx1 -= dx;
        }
        if (dx + srcx2 >= 256) {
            srcx2 = 256 - dx;
        }

        if (dy < 0) {
            srcy1 -= dy;
        }
        if (dy + srcy2 >= 240) {
            srcy2 = 240 - dy;
        }

        if (!flipHorizontal && !flipVertical) {

            fbIndex = (dy << 8) + dx;
            tIndex = 0;
            for (y = 0; y < 8; y++) {
                for (x = 0; x < 8; x++) {
                    if (x >= srcx1 && x < srcx2 && y >= srcy1 && y < srcy2) {
                        palIndex = pix[tIndex];
                        tpri = priTable[fbIndex];
                        if (palIndex != 0 && pri <= (tpri & 0xFF)) {
                            fBuffer[fbIndex] = palette[palIndex + palAdd];
                            tpri = (tpri & 0xF00) | pri;
                            priTable[fbIndex] = tpri;
                        }
                    }
                    fbIndex++;
                    tIndex++;
                }
                fbIndex -= 8;
                fbIndex += 256;
            }

        } else if (flipHorizontal && !flipVertical) {

            fbIndex = (dy << 8) + dx;
            tIndex = 7;
            for (y = 0; y < 8; y++) {
                for (x = 0; x < 8; x++) {
                    if (x >= srcx1 && x < srcx2 && y >= srcy1 && y < srcy2) {
                        palIndex = pix[tIndex];
                        tpri = priTable[fbIndex];
                        if (palIndex != 0 && pri <= (tpri & 0xFF)) {
                            fBuffer[fbIndex] = palette[palIndex + palAdd];
                            tpri = (tpri & 0xF00) | pri;
                            priTable[fbIndex] = tpri;
                        }
                    }
                    fbIndex++;
                    tIndex--;
                }
                fbIndex -= 8;
                fbIndex += 256;
                tIndex += 16;
            }

        } else if (flipVertical && !flipHorizontal) {

            fbIndex = (dy << 8) + dx;
            tIndex = 56;
            for (y = 0; y < 8; y++) {
                for (x = 0; x < 8; x++) {
                    if (x >= srcx1 && x < srcx2 && y >= srcy1 && y < srcy2) {
                        palIndex = pix[tIndex];
                        tpri = priTable[fbIndex];
                        if (palIndex != 0 && pri <= (tpri & 0xFF)) {
                            fBuffer[fbIndex] = palette[palIndex + palAdd];
                            tpri = (tpri & 0xF00) | pri;
                            priTable[fbIndex] = tpri;
                        }
                    }
                    fbIndex++;
                    tIndex++;
                }
                fbIndex -= 8;
                fbIndex += 256;
                tIndex -= 16;
            }

        } else {

            fbIndex = (dy << 8) + dx;
            tIndex = 63;
            for (y = 0; y < 8; y++) {
                for (x = 0; x < 8; x++) {
                    if (x >= srcx1 && x < srcx2 && y >= srcy1 && y < srcy2) {
                        palIndex = pix[tIndex];
                        tpri = priTable[fbIndex];
                        if (palIndex != 0 && pri <= (tpri & 0xFF)) {
                            fBuffer[fbIndex] = palette[palIndex + palAdd];
                            tpri = (tpri & 0xF00) | pri;
                            priTable[fbIndex] = tpri;
                        }
                    }
                    fbIndex++;
                    tIndex--;
                }
                fbIndex -= 8;
                fbIndex += 256;
            }

        }

    }

    public boolean isTransparent(int x, int y) {
        return (pix[(y << 3) + x] == 0);
    }

    public void dumpData(String file) {

        try {

            File f = new File(file);
            FileWriter fWriter = new FileWriter(f);

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    fWriter.write(Misc.hex8(pix[(y << 3) + x]).substring(1));
                }
                fWriter.write("\r\n");
            }

            fWriter.close();
        //System.out.println("Tile data dumped to file "+file);

        } catch (Exception e) {
            //System.out.println("Unable to dump tile to file.");
            e.printStackTrace();
        }
    }

    public void stateSave(ByteBuffer buf) {

        buf.putBoolean(initialized);
        for (int i = 0; i < 8; i++) {
            buf.putBoolean(opaque[i]);
        }
        for (int i = 0; i < 64; i++) {
            buf.putByte((byte) pix[i]);
        }

    }

    public void stateLoad(ByteBuffer buf) {

        initialized = buf.readBoolean();
        for (int i = 0; i < 8; i++) {
            opaque[i] = buf.readBoolean();
        }
        for (int i = 0; i < 64; i++) {
            pix[i] = buf.readByte();
        }

    }
}