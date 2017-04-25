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

public interface MemoryMapper {

    public void init(NES nes);

    public void loadROM(ROM rom);

    public void write(int address, short value);

    public short load(int address);

    public short joy1Read();

    public short joy2Read();

    public void reset();

    public void setGameGenieState(boolean value);

    public void clockIrqCounter();

    public void loadBatteryRam();

    public void destroy();

    public void stateLoad(ByteBuffer buf);

    public void stateSave(ByteBuffer buf);

    public void setMouseState(boolean pressed, int x, int y);

    public void latchAccess(int address);
}