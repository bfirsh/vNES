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

public interface InputHandler {

    // Joypad keys:
    public static final int KEY_A = 0;
    public static final int KEY_B = 1;
    public static final int KEY_START = 2;
    public static final int KEY_SELECT = 3;
    public static final int KEY_UP = 4;
    public static final int KEY_DOWN = 5;
    public static final int KEY_LEFT = 6;
    public static final int KEY_RIGHT = 7;
    
    // Key count:
    public static final int NUM_KEYS = 8;

    public short getKeyState(int padKey);

    public void mapKey(int padKey, int deviceKey);

    public void reset();

    public void update();
}