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

public class Raster {

    public int[] data;
    public int width;
    public int height;

    public Raster(int[] data, int w, int h) {
        this.data = data;
        width = w;
        height = h;
    }

    public Raster(int w, int h) {
        data = new int[w * h];
        width = w;
        height = h;
    }

    public void drawTile(Raster srcRaster, int srcx, int srcy, int dstx, int dsty, int w, int h) {

        int[] src = srcRaster.data;
        int src_index;
        int dst_index;
        int tmp;

        for (int y = 0; y < h; y++) {

            src_index = (srcy + y) * srcRaster.width + srcx;
            dst_index = (dsty + y) * width + dstx;

            for (int x = 0; x < w; x++) {

                if ((tmp = src[src_index]) != 0) {
                    data[dst_index] = tmp;
                }

                src_index++;
                dst_index++;

            }
        }

    }
}