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

public class HiResTimer {

    public long currentMicros() {
        return System.nanoTime() / 1000;
    }

    public long currentTick() {
        return System.nanoTime();
    }

    public void sleepMicros(long time) {

        try {

            long nanos = time - (time / 1000) * 1000;
            if (nanos > 999999) {
                nanos = 999999;
            }
            Thread.sleep(time / 1000, (int) nanos);

        } catch (Exception e) {

            //System.out.println("Sleep interrupted..");
            e.printStackTrace();

        }

    }

    public void sleepMillisIdle(int millis) {

        millis /= 10;
        millis *= 10;

        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
        }

    }

    public void yield() {
        Thread.yield();
    }
}