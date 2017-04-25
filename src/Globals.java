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

import java.util.*;

public class Globals {

    public static double CPU_FREQ_NTSC = 1789772.5d;
    public static double CPU_FREQ_PAL = 1773447.4d;
    public static int preferredFrameRate = 60;
    
    // Microseconds per frame:
    public static int frameTime = 1000000 / preferredFrameRate;
    // What value to flush memory with on power-up:
    public static short memoryFlushValue = 0xFF;

    public static final boolean debug = true;
    public static final boolean fsdebug = false;

    public static boolean appletMode = true;
    public static boolean disableSprites = false;
    public static boolean timeEmulation = true;
    public static boolean palEmulation;
    public static boolean enableSound = true;
    public static boolean focused = false;

    public static HashMap keycodes = new HashMap(); //Java key codes
    public static HashMap controls = new HashMap(); //vNES controls codes

    public static NES nes;

    public static void println(String s) {
        nes.getGui().println(s);
    }
}