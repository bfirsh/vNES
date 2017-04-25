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

public class ChannelTriangle implements PapuChannel {

    PAPU papu;
    boolean isEnabled;
    boolean sampleCondition;
    boolean lengthCounterEnable;
    boolean lcHalt;
    boolean lcControl;
    int progTimerCount;
    int progTimerMax;
    int triangleCounter;
    int lengthCounter;
    int linearCounter;
    int lcLoadValue;
    int sampleValue;
    int tmp;

    public ChannelTriangle(PAPU papu) {
        this.papu = papu;
    }

    public void clockLengthCounter() {
        if (lengthCounterEnable && lengthCounter > 0) {
            lengthCounter--;
            if (lengthCounter == 0) {
                updateSampleCondition();
            }
        }
    }

    public void clockLinearCounter() {

        if (lcHalt) {

            // Load:
            linearCounter = lcLoadValue;
            updateSampleCondition();

        } else if (linearCounter > 0) {

            // Decrement:
            linearCounter--;
            updateSampleCondition();

        }

        if (!lcControl) {

            // Clear halt flag:
            lcHalt = false;

        }

    }

    public int getLengthStatus() {
        return ((lengthCounter == 0 || !isEnabled) ? 0 : 1);
    }

    public int readReg(int address) {
        return 0;
    }

    public void writeReg(int address, int value) {

        if (address == 0x4008) {

            // New values for linear counter:
            lcControl = (value & 0x80) != 0;
            lcLoadValue = value & 0x7F;

            // Length counter enable:
            lengthCounterEnable = !lcControl;

        } else if (address == 0x400A) {

            // Programmable timer:
            progTimerMax &= 0x700;
            progTimerMax |= value;

        } else if (address == 0x400B) {

            // Programmable timer, length counter
            progTimerMax &= 0xFF;
            progTimerMax |= ((value & 0x07) << 8);
            lengthCounter = papu.getLengthMax(value & 0xF8);
            lcHalt = true;

        }

        updateSampleCondition();

    }

    public void clockProgrammableTimer(int nCycles) {

        if (progTimerMax > 0) {
            progTimerCount += nCycles;
            while (progTimerMax > 0 && progTimerCount >= progTimerMax) {
                progTimerCount -= progTimerMax;
                if (isEnabled && lengthCounter > 0 && linearCounter > 0) {
                    clockTriangleGenerator();
                }
            }
        }

    }

    public void clockTriangleGenerator() {
        triangleCounter++;
        triangleCounter &= 0x1F;
    }

    public void setEnabled(boolean value) {
        isEnabled = value;
        if (!value) {
            lengthCounter = 0;
        }
        updateSampleCondition();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void updateSampleCondition() {
        sampleCondition =
                isEnabled &&
                progTimerMax > 7 &&
                linearCounter > 0 &&
                lengthCounter > 0;
    }

    public void reset() {

        progTimerCount = 0;
        progTimerMax = 0;
        triangleCounter = 0;
        isEnabled = false;
        sampleCondition = false;
        lengthCounter = 0;
        lengthCounterEnable = false;
        linearCounter = 0;
        lcLoadValue = 0;
        lcHalt = true;
        lcControl = false;
        tmp = 0;
        sampleValue = 0xF;

    }

    public void destroy() {
        papu = null;
    }
}