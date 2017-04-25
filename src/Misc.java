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

public class Misc {

    public static boolean debug = Globals.debug;
    private static float[] rnd = new float[100000];
    private static int nextRnd = 0;
    private static float rndret;


    static {
        for (int i = 0; i < rnd.length; i++) {
            rnd[i] = (float) Math.random();
        }
    }

    public static String hex8(int i) {
        String s = Integer.toHexString(i);
        while (s.length() < 2) {
            s = "0" + s;
        }
        return s.toUpperCase();
    }

    public static String hex16(int i) {
        String s = Integer.toHexString(i);
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s.toUpperCase();
    }

    public static String binN(int num, int N) {
        char[] c = new char[N];
        for (int i = 0; i < N; i++) {
            c[N - i - 1] = (num & 0x1) == 1 ? '1' : '0';
            num >>= 1;
        }
        return new String(c);
    }

    public static String bin8(int num) {
        return binN(num, 8);
    }

    public static String bin16(int num) {
        return binN(num, 16);
    }

    public static String binStr(long value, int bitcount) {
        String ret = "";
        for (int i = 0; i < bitcount; i++) {
            ret = ((value & (1 << i)) != 0 ? "1" : "0") + ret;
        }
        return ret;
    }

    public static int[] resizeArray(int[] array, int newSize) {

        int[] newArr = new int[newSize];
        System.arraycopy(array, 0, newArr, 0, Math.min(newSize, array.length));
        return newArr;

    }

    public static String pad(String str, String padStr, int length) {
        while (str.length() < length) {
            str += padStr;
        }
        return str;
    }

    public static float random() {
        rndret = rnd[nextRnd];
        nextRnd++;
        if (nextRnd >= rnd.length) {
            nextRnd = (int) (Math.random() * (rnd.length - 1));
        }
        return rndret;
    }
}