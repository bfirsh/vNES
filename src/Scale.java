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

public class Scale {

    private static int brightenShift;
    private static int brightenShiftMask;
    private static int brightenCutoffMask;
    private static int darkenShift;
    private static int darkenShiftMask;
    private static int si,  di,  di2,  val,  x,  y;

    public static void setFilterParams(int darkenDepth, int brightenDepth) {

        switch (darkenDepth) {
            case 0: {
                darkenShift = 0;
                darkenShiftMask = 0x00000000;
                break;
            }
            case 1: {
                darkenShift = 4;
                darkenShiftMask = 0x000F0F0F;
                break;
            }
            case 2: {
                darkenShift = 3;
                darkenShiftMask = 0x001F1F1F;
                break;
            }
            case 3: {
                darkenShift = 2;
                darkenShiftMask = 0x003F3F3F;
                break;
            }
            default: {
                darkenShift = 1;
                darkenShiftMask = 0x007F7F7F;
                break;
            }
        }

        switch (brightenDepth) {
            case 0: {
                brightenShift = 0;
                brightenShiftMask = 0x00000000;
                brightenCutoffMask = 0x00000000;
                break;
            }
            case 1: {
                brightenShift = 4;
                brightenShiftMask = 0x000F0F0F;
                brightenCutoffMask = 0x003F3F3F;
                break;
            }
            case 2: {
                brightenShift = 3;
                brightenShiftMask = 0x001F1F1F;
                brightenCutoffMask = 0x003F3F3F;
                break;
            }
            case 3: {
                brightenShift = 2;
                brightenShiftMask = 0x003F3F3F;
                brightenCutoffMask = 0x007F7F7F;
                break;
            }
            default: {
                brightenShift = 1;
                brightenShiftMask = 0x007F7F7F;
                brightenCutoffMask = 0x007F7F7F;
                break;
            }
        }

    }

    public static final void doScanlineScaling(int[] src, int[] dest, boolean[] changed) {

        int di = 0;
        int di2 = 512;
        int val, max;

        for (int y = 0; y < 240; y++) {
            if (changed[y]) {
                max = (y + 1) << 8;
                for (int si = y << 8; si < max; si++) {

                    // get pixel value:
                    val = src[si];

                    // fill the two pixels on the current scanline:
                    dest[di] = val;
                    dest[++di] = val;

                    // darken pixel:
                    val -= ((val >> 2) & 0x003F3F3F);

                    // fill the two pixels on the next scanline:
                    dest[di2] = val;
                    dest[++di2] = val;

                    //si ++;
                    di++;
                    di2++;

                }
            } else {
                di += 512;
                di2 += 512;
            }

            // skip one scanline:
            di += 512;
            di2 += 512;

        }

    }

    public static final void doRasterScaling(int[] src, int[] dest, boolean[] changed) {

        int di = 0;
        int di2 = 512;

        int max;
        int col1, col2, col3;
        int r, g, b;
        int flag = 0;

        for (int y = 0; y < 240; y++) {
            if (changed[y]) {
                max = (y + 1) << 8;
                for (int si = y << 8; si < max; si++) {

                    // get pixel value:
                    col1 = src[si];

                    // fill the two pixels on the current scanline:
                    dest[di] = col1;
                    dest[++di] = col1;

                    // fill the two pixels on the next scanline:
                    dest[di2] = col1;
                    dest[++di2] = col1;

                    // darken pixel:
                    col2 = col1 - ((col1 >> darkenShift) & darkenShiftMask);

                    // brighten pixel:
                    col3 = col1 +
                            ((((0x00FFFFFF - col1) & brightenCutoffMask) >> brightenShift) & brightenShiftMask);

                    dest[di + (512 & flag)] = col2;
                    dest[di + (512 & flag) - 1] = col2;
                    dest[di + 512 & (512 - flag)] = col3;
                    flag = 512 - flag;

                    di++;
                    di2++;

                }
            } else {
                di += 512;
                di2 += 512;
            }

            // skip one scanline:
            di += 512;
            di2 += 512;

        }

    }

    public static final void doNormalScaling(int[] src, int[] dest, boolean[] changed) {

        int di = 0;
        int di2 = 512;
        int val, max;

        for (int y = 0; y < 240; y++) {
            if (changed[y]) {
                max = (y + 1) << 8;
                for (int si = y << 8; si < max; si++) {

                    // get pixel value:
                    val = src[si];

                    // fill the two pixels on the current scanline:
                    dest[di++] = val;
                    dest[di++] = val;

                    // fill the two pixels on the next scanline:
                    dest[di2++] = val;
                    dest[di2++] = val;

                }
            } else {
                di += 512;
                di2 += 512;
            }

            // skip one scanline:
            di += 512;
            di2 += 512;

        }

    }
}