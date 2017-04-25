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

import javax.sound.sampled.*;

public final class PAPU {

    NES nes;
    Memory cpuMem;
    Mixer mixer;
    SourceDataLine line;
    ChannelSquare square1;
    ChannelSquare square2;
    ChannelTriangle triangle;
    ChannelNoise noise;
    ChannelDM dmc;
    int[] lengthLookup;
    int[] dmcFreqLookup;
    int[] noiseWavelengthLookup;
    int[] square_table;
    int[] tnd_table;
    int[] ismpbuffer;
    byte[] sampleBuffer;
    int frameIrqCounter;
    int frameIrqCounterMax;
    int initCounter;
    short channelEnableValue;
    byte b1, b2, b3, b4;
    int bufferSize = 2048;
    int bufferIndex;
    int sampleRate = 44100;
    boolean frameIrqEnabled;
    boolean frameIrqActive;
    boolean frameClockNow;
    boolean startedPlaying = false;
    boolean recordOutput = false;
    boolean stereo = true;
    boolean initingHardware = false;
    private boolean userEnableSquare1 = true;
    private boolean userEnableSquare2 = true;
    private boolean userEnableTriangle = true;
    private boolean userEnableNoise = true;
    public boolean userEnableDmc = true;
    int masterFrameCounter;
    int derivedFrameCounter;
    int countSequence;
    int sampleTimer;
    int frameTime;
    int sampleTimerMax;
    int sampleCount;
    int sampleValueL, sampleValueR;
    int triValue = 0;
    int smpSquare1, smpSquare2, smpTriangle, smpNoise, smpDmc;
    int accCount;
    int sq_index, tnd_index;

    // DC removal vars:
    int prevSampleL = 0, prevSampleR = 0;
    int smpAccumL = 0, smpAccumR = 0;
    int smpDiffL = 0, smpDiffR = 0;

    // DAC range:
    int dacRange = 0;
    int dcValue = 0;

    // Master volume:
    int masterVolume;

    // Panning:
    int[] panning;

    // Stereo positioning:
    int stereoPosLSquare1;
    int stereoPosLSquare2;
    int stereoPosLTriangle;
    int stereoPosLNoise;
    int stereoPosLDMC;
    int stereoPosRSquare1;
    int stereoPosRSquare2;
    int stereoPosRTriangle;
    int stereoPosRNoise;
    int stereoPosRDMC;
    int extraCycles;
    int maxCycles;

    public PAPU(NES nes) {

        this.nes = nes;
        cpuMem = nes.getCpuMemory();

        setSampleRate(sampleRate, false);
        sampleBuffer = new byte[bufferSize * (stereo ? 4 : 2)];
        ismpbuffer = new int[bufferSize * (stereo ? 2 : 1)];
        bufferIndex = 0;
        frameIrqEnabled = false;
        initCounter = 2048;

        square1 = new ChannelSquare(this, true);
        square2 = new ChannelSquare(this, false);
        triangle = new ChannelTriangle(this);
        noise = new ChannelNoise(this);
        dmc = new ChannelDM(this);

        masterVolume = 256;
        panning = new int[]{
                    80,
                    170,
                    100,
                    150,
                    128
                };
        setPanning(panning);

        // Initialize lookup tables:
        initLengthLookup();
        initDmcFrequencyLookup();
        initNoiseWavelengthLookup();
        initDACtables();

        frameIrqEnabled = false;
        frameIrqCounterMax = 4;

    }

    public void stateLoad(ByteBuffer buf) {
        // not yet.
    }

    public void stateSave(ByteBuffer buf) {
        // not yet.
    }

    public synchronized void start() {

        //System.out.println("* Starting PAPU lines.");
        if (line != null && line.isActive()) {
            //System.out.println("* Already running.");
            return;
        }

        bufferIndex = 0;
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();

        if (mixerInfo == null || mixerInfo.length == 0) {
            //System.out.println("No audio mixer available, sound disabled.");
            Globals.enableSound = false;
            return;
        }

        mixer = AudioSystem.getMixer(mixerInfo[1]);

        AudioFormat audioFormat = new AudioFormat(sampleRate, 16, (stereo ? 2 : 1), true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, sampleRate);

        try {

            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();

        } catch (Exception e) {
            //System.out.println("Couldn't get sound lines.");
        }

    }

    public NES getNes() {
        return nes;
    }

    public short readReg(int address) {

        // Read 0x4015:
        int tmp = 0;
        tmp |= (square1.getLengthStatus());
        tmp |= (square2.getLengthStatus() << 1);
        tmp |= (triangle.getLengthStatus() << 2);
        tmp |= (noise.getLengthStatus() << 3);
        tmp |= (dmc.getLengthStatus() << 4);
        tmp |= (((frameIrqActive && frameIrqEnabled) ? 1 : 0) << 6);
        tmp |= (dmc.getIrqStatus() << 7);

        frameIrqActive = false;
        dmc.irqGenerated = false;

        ////System.out.println("$4015 read. Value = "+Misc.bin8(tmp)+" countseq = "+countSequence);
        return (short) tmp;

    }

    public void writeReg(int address, short value) {

        if (address >= 0x4000 && address < 0x4004) {

            // Square Wave 1 Control
            square1.writeReg(address, value);
        ////System.out.println("Square Write");

        } else if (address >= 0x4004 && address < 0x4008) {

            // Square 2 Control
            square2.writeReg(address, value);

        } else if (address >= 0x4008 && address < 0x400C) {

            // Triangle Control
            triangle.writeReg(address, value);

        } else if (address >= 0x400C && address <= 0x400F) {

            // Noise Control
            noise.writeReg(address, value);

        } else if (address == 0x4010) {

            // DMC Play mode & DMA frequency
            dmc.writeReg(address, value);

        } else if (address == 0x4011) {

            // DMC Delta Counter
            dmc.writeReg(address, value);

        } else if (address == 0x4012) {

            // DMC Play code starting address
            dmc.writeReg(address, value);

        } else if (address == 0x4013) {

            // DMC Play code length
            dmc.writeReg(address, value);

        } else if (address == 0x4015) {

            // Channel enable
            updateChannelEnable(value);

            if (value != 0 && initCounter > 0) {

                // Start hardware initialization
                initingHardware = true;

            }

            // DMC/IRQ Status
            dmc.writeReg(address, value);

        } else if (address == 0x4017) {


            // Frame counter control
            countSequence = (value >> 7) & 1;
            masterFrameCounter = 0;
            frameIrqActive = false;

            if (((value >> 6) & 0x1) == 0) {
                frameIrqEnabled = true;
            } else {
                frameIrqEnabled = false;
            }

            if (countSequence == 0) {

                // NTSC:
                frameIrqCounterMax = 4;
                derivedFrameCounter = 4;

            } else {

                // PAL:
                frameIrqCounterMax = 5;
                derivedFrameCounter = 0;
                frameCounterTick();

            }

        }
    }

    public void resetCounter() {

        if (countSequence == 0) {
            derivedFrameCounter = 4;
        } else {
            derivedFrameCounter = 0;
        }

    }


    // Updates channel enable status.
    // This is done on writes to the
    // channel enable register (0x4015),
    // and when the user enables/disables channels
    // in the GUI.
    public void updateChannelEnable(int value) {

        channelEnableValue = (short) value;
        square1.setEnabled(userEnableSquare1 && (value & 1) != 0);
        square2.setEnabled(userEnableSquare2 && (value & 2) != 0);
        triangle.setEnabled(userEnableTriangle && (value & 4) != 0);
        noise.setEnabled(userEnableNoise && (value & 8) != 0);
        dmc.setEnabled(userEnableDmc && (value & 16) != 0);

    }

    // Clocks the frame counter. It should be clocked at
    // twice the cpu speed, so the cycles will be
    // divided by 2 for those counters that are
    // clocked at cpu speed.
    public void clockFrameCounter(int nCycles) {

        if (initCounter > 0) {
            if (initingHardware) {
                initCounter -= nCycles;
                if (initCounter <= 0) {
                    initingHardware = false;
                }
                return;
            }
        }

        // Don't process ticks beyond next sampling:
        nCycles += extraCycles;
        maxCycles = sampleTimerMax - sampleTimer;
        if ((nCycles << 10) > maxCycles) {

            extraCycles = ((nCycles << 10) - maxCycles) >> 10;
            nCycles -= extraCycles;

        } else {

            extraCycles = 0;

        }

        // Clock DMC:
        if (dmc.isEnabled) {

            dmc.shiftCounter -= (nCycles << 3);
            while (dmc.shiftCounter <= 0 && dmc.dmaFrequency > 0) {
                dmc.shiftCounter += dmc.dmaFrequency;
                dmc.clockDmc();
            }

        }

        // Clock Triangle channel Prog timer:
        if (triangle.progTimerMax > 0) {

            triangle.progTimerCount -= nCycles;
            while (triangle.progTimerCount <= 0) {

                triangle.progTimerCount += triangle.progTimerMax + 1;
                if (triangle.linearCounter > 0 && triangle.lengthCounter > 0) {

                    triangle.triangleCounter++;
                    triangle.triangleCounter &= 0x1F;

                    if (triangle.isEnabled) {
                        if (triangle.triangleCounter >= 0x10) {
                            // Normal value.
                            triangle.sampleValue = (triangle.triangleCounter & 0xF);
                        } else {
                            // Inverted value.
                            triangle.sampleValue = (0xF - (triangle.triangleCounter & 0xF));
                        }
                        triangle.sampleValue <<= 4;
                    }

                }
            }

        }

        // Clock Square channel 1 Prog timer:
        square1.progTimerCount -= nCycles;
        if (square1.progTimerCount <= 0) {

            square1.progTimerCount += (square1.progTimerMax + 1) << 1;

            square1.squareCounter++;
            square1.squareCounter &= 0x7;
            square1.updateSampleValue();

        }

        // Clock Square channel 2 Prog timer:
        square2.progTimerCount -= nCycles;
        if (square2.progTimerCount <= 0) {

            square2.progTimerCount += (square2.progTimerMax + 1) << 1;

            square2.squareCounter++;
            square2.squareCounter &= 0x7;
            square2.updateSampleValue();

        }

        // Clock noise channel Prog timer:
        int acc_c = nCycles;
        if (noise.progTimerCount - acc_c > 0) {

            // Do all cycles at once:
            noise.progTimerCount -= acc_c;
            noise.accCount += acc_c;
            noise.accValue += acc_c * noise.sampleValue;

        } else {

            // Slow-step:
            while ((acc_c--) > 0) {

                if (--noise.progTimerCount <= 0 && noise.progTimerMax > 0) {

                    // Update noise shift register:
                    noise.shiftReg <<= 1;
                    noise.tmp = (((noise.shiftReg << (noise.randomMode == 0 ? 1 : 6)) ^ noise.shiftReg) & 0x8000);
                    if (noise.tmp != 0) {

                        // Sample value must be 0.
                        noise.shiftReg |= 0x01;
                        noise.randomBit = 0;
                        noise.sampleValue = 0;

                    } else {

                        // Find sample value:
                        noise.randomBit = 1;
                        if (noise.isEnabled && noise.lengthCounter > 0) {
                            noise.sampleValue = noise.masterVolume;
                        } else {
                            noise.sampleValue = 0;
                        }

                    }

                    noise.progTimerCount += noise.progTimerMax;

                }

                noise.accValue += noise.sampleValue;
                noise.accCount++;

            }
        }


        // Frame IRQ handling:
        if (frameIrqEnabled && frameIrqActive) {
            nes.cpu.requestIrq(CPU.IRQ_NORMAL);
        }

        // Clock frame counter at double CPU speed:
        masterFrameCounter += (nCycles << 1);
        if (masterFrameCounter >= frameTime) {

            // 240Hz tick:
            masterFrameCounter -= frameTime;
            frameCounterTick();


        }


        // Accumulate sample value:
        accSample(nCycles);


        // Clock sample timer:
        sampleTimer += nCycles << 10;
        if (sampleTimer >= sampleTimerMax) {

            // Sample channels:
            sample();
            sampleTimer -= sampleTimerMax;

        }

    }

    private void accSample(int cycles) {

        // Special treatment for triangle channel - need to interpolate.
        if (triangle.sampleCondition) {

            triValue = (triangle.progTimerCount << 4) / (triangle.progTimerMax + 1);
            if (triValue > 16) {
                triValue = 16;
            }
            if (triangle.triangleCounter >= 16) {
                triValue = 16 - triValue;
            }

            // Add non-interpolated sample value:
            triValue += triangle.sampleValue;

        }


        // Now sample normally:
        if (cycles == 2) {

            smpTriangle += triValue << 1;
            smpDmc += dmc.sample << 1;
            smpSquare1 += square1.sampleValue << 1;
            smpSquare2 += square2.sampleValue << 1;
            accCount += 2;

        } else if (cycles == 4) {

            smpTriangle += triValue << 2;
            smpDmc += dmc.sample << 2;
            smpSquare1 += square1.sampleValue << 2;
            smpSquare2 += square2.sampleValue << 2;
            accCount += 4;

        } else {

            smpTriangle += cycles * triValue;
            smpDmc += cycles * dmc.sample;
            smpSquare1 += cycles * square1.sampleValue;
            smpSquare2 += cycles * square2.sampleValue;
            accCount += cycles;

        }

    }

    public void frameCounterTick() {

        derivedFrameCounter++;
        if (derivedFrameCounter >= frameIrqCounterMax) {
            derivedFrameCounter = 0;
        }

        if (derivedFrameCounter == 1 || derivedFrameCounter == 3) {

            // Clock length & sweep:
            triangle.clockLengthCounter();
            square1.clockLengthCounter();
            square2.clockLengthCounter();
            noise.clockLengthCounter();
            square1.clockSweep();
            square2.clockSweep();

        }

        if (derivedFrameCounter >= 0 && derivedFrameCounter < 4) {

            // Clock linear & decay:
            square1.clockEnvDecay();
            square2.clockEnvDecay();
            noise.clockEnvDecay();
            triangle.clockLinearCounter();

        }

        if (derivedFrameCounter == 3 && countSequence == 0) {

            // Enable IRQ:
            frameIrqActive = true;

        }


    // End of 240Hz tick

    }


    // Samples the channels, mixes the output together,
    // writes to buffer and (if enabled) file.
    public void sample() {

        if (accCount > 0) {

            smpSquare1 <<= 4;
            smpSquare1 /= accCount;

            smpSquare2 <<= 4;
            smpSquare2 /= accCount;

            smpTriangle /= accCount;

            smpDmc <<= 4;
            smpDmc /= accCount;

            accCount = 0;

        } else {

            smpSquare1 = square1.sampleValue << 4;
            smpSquare2 = square2.sampleValue << 4;
            smpTriangle = triangle.sampleValue;
            smpDmc = dmc.sample << 4;

        }

        smpNoise = (int) ((noise.accValue << 4) / noise.accCount);
        noise.accValue = smpNoise >> 4;
        noise.accCount = 1;

        if (stereo) {

            // Stereo sound.

            // Left channel:
            sq_index = (smpSquare1 * stereoPosLSquare1 + smpSquare2 * stereoPosLSquare2) >> 8;
            tnd_index = (3 * smpTriangle * stereoPosLTriangle + (smpNoise << 1) * stereoPosLNoise + smpDmc * stereoPosLDMC) >> 8;
            if (sq_index >= square_table.length) {
                sq_index = square_table.length - 1;
            }
            if (tnd_index >= tnd_table.length) {
                tnd_index = tnd_table.length - 1;
            }
            sampleValueL = square_table[sq_index] + tnd_table[tnd_index] - dcValue;

            // Right channel:
            sq_index = (smpSquare1 * stereoPosRSquare1 + smpSquare2 * stereoPosRSquare2) >> 8;
            tnd_index = (3 * smpTriangle * stereoPosRTriangle + (smpNoise << 1) * stereoPosRNoise + smpDmc * stereoPosRDMC) >> 8;
            if (sq_index >= square_table.length) {
                sq_index = square_table.length - 1;
            }
            if (tnd_index >= tnd_table.length) {
                tnd_index = tnd_table.length - 1;
            }
            sampleValueR = square_table[sq_index] + tnd_table[tnd_index] - dcValue;

        } else {

            // Mono sound:
            sq_index = smpSquare1 + smpSquare2;
            tnd_index = 3 * smpTriangle + 2 * smpNoise + smpDmc;
            if (sq_index >= square_table.length) {
                sq_index = square_table.length - 1;
            }
            if (tnd_index >= tnd_table.length) {
                tnd_index = tnd_table.length - 1;
            }
            sampleValueL = 3 * (square_table[sq_index] + tnd_table[tnd_index] - dcValue);
            sampleValueL >>= 2;

        }

        // Remove DC from left channel:
        smpDiffL = sampleValueL - prevSampleL;
        prevSampleL += smpDiffL;
        smpAccumL += smpDiffL - (smpAccumL >> 10);
        sampleValueL = smpAccumL;

        if (stereo) {

            // Remove DC from right channel:
            smpDiffR = sampleValueR - prevSampleR;
            prevSampleR += smpDiffR;
            smpAccumR += smpDiffR - (smpAccumR >> 10);
            sampleValueR = smpAccumR;

            // Write:
            if (bufferIndex + 4 < sampleBuffer.length) {

                sampleBuffer[bufferIndex++] = (byte) ((sampleValueL) & 0xFF);
                sampleBuffer[bufferIndex++] = (byte) ((sampleValueL >> 8) & 0xFF);
                sampleBuffer[bufferIndex++] = (byte) ((sampleValueR) & 0xFF);
                sampleBuffer[bufferIndex++] = (byte) ((sampleValueR >> 8) & 0xFF);

            }


        } else {

            // Write:
            if (bufferIndex + 2 < sampleBuffer.length) {

                sampleBuffer[bufferIndex++] = (byte) ((sampleValueL) & 0xFF);
                sampleBuffer[bufferIndex++] = (byte) ((sampleValueL >> 8) & 0xFF);

            }

        }
        // Reset sampled values:
        smpSquare1 = 0;
        smpSquare2 = 0;
        smpTriangle = 0;
        smpDmc = 0;

    }


    // Writes the sound buffer to the output line:
    public void writeBuffer() {

        if (line == null) {
            return;
        }
        bufferIndex -= (bufferIndex % (stereo ? 4 : 2));
        line.write(sampleBuffer, 0, bufferIndex);

        bufferIndex = 0;

    }

    public void stop() {

        if (line == null) {
            // No line to close. Probably lack of sound card.
            return;
        }

        if (line != null && line.isOpen() && line.isActive()) {
            line.close();
        }

        // Lose line:
        line = null;

    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void reset() {

        setSampleRate(sampleRate, false);
        updateChannelEnable(0);
        masterFrameCounter = 0;
        derivedFrameCounter = 0;
        countSequence = 0;
        sampleCount = 0;
        initCounter = 2048;
        frameIrqEnabled = false;
        initingHardware = false;

        resetCounter();

        square1.reset();
        square2.reset();
        triangle.reset();
        noise.reset();
        dmc.reset();

        bufferIndex = 0;
        accCount = 0;
        smpSquare1 = 0;
        smpSquare2 = 0;
        smpTriangle = 0;
        smpNoise = 0;
        smpDmc = 0;

        frameIrqEnabled = false;
        frameIrqCounterMax = 4;

        channelEnableValue = 0xFF;
        b1 = 0;
        b2 = 0;
        startedPlaying = false;
        sampleValueL = 0;
        sampleValueR = 0;
        prevSampleL = 0;
        prevSampleR = 0;
        smpAccumL = 0;
        smpAccumR = 0;
        smpDiffL = 0;
        smpDiffR = 0;

    }

    public int getLengthMax(int value) {
        return lengthLookup[value >> 3];
    }

    public int getDmcFrequency(int value) {
        if (value >= 0 && value < 0x10) {
            return dmcFreqLookup[value];
        }
        return 0;
    }

    public int getNoiseWaveLength(int value) {
        if (value >= 0 && value < 0x10) {
            return noiseWavelengthLookup[value];
        }
        return 0;
    }

    public synchronized void setSampleRate(int rate, boolean restart) {

        boolean cpuRunning = nes.isRunning();
        if (cpuRunning) {
            nes.stopEmulation();
        }

        sampleRate = rate;
        sampleTimerMax = (int) ((1024.0 * Globals.CPU_FREQ_NTSC * Globals.preferredFrameRate) /
                (sampleRate * 60.0d));

        frameTime = (int) ((14915.0 * (double) Globals.preferredFrameRate) / 60.0d);

        sampleTimer = 0;
        bufferIndex = 0;

        if (restart) {
            stop();
            start();
        }

        if (cpuRunning) {
            nes.startEmulation();
        }

    }

    public synchronized void setStereo(boolean s, boolean restart) {

        if (stereo == s) {
            return;
        }

        boolean running = nes.isRunning();
        nes.stopEmulation();

        stereo = s;
        if (stereo) {
            sampleBuffer = new byte[bufferSize * 4];
        } else {
            sampleBuffer = new byte[bufferSize * 2];
        }

        if (restart) {
            stop();
            start();
        }

        if (running) {
            nes.startEmulation();
        }

    }

    public int getPapuBufferSize() {
        return sampleBuffer.length;
    }

    public void setChannelEnabled(int channel, boolean value) {
        if (channel == 0) {
            userEnableSquare1 = value;
        } else if (channel == 1) {
            userEnableSquare2 = value;
        } else if (channel == 2) {
            userEnableTriangle = value;
        } else if (channel == 3) {
            userEnableNoise = value;
        } else {
            userEnableDmc = value;
        }
        updateChannelEnable(channelEnableValue);
    }

    public void setPanning(int[] pos) {

        for (int i = 0; i < 5; i++) {
            panning[i] = pos[i];
        }
        updateStereoPos();

    }

    public void setMasterVolume(int value) {

        if (value < 0) {
            value = 0;
        }
        if (value > 256) {
            value = 256;
        }
        masterVolume = value;
        updateStereoPos();

    }

    public void updateStereoPos() {

        stereoPosLSquare1 = (panning[0] * masterVolume) >> 8;
        stereoPosLSquare2 = (panning[1] * masterVolume) >> 8;
        stereoPosLTriangle = (panning[2] * masterVolume) >> 8;
        stereoPosLNoise = (panning[3] * masterVolume) >> 8;
        stereoPosLDMC = (panning[4] * masterVolume) >> 8;

        stereoPosRSquare1 = masterVolume - stereoPosLSquare1;
        stereoPosRSquare2 = masterVolume - stereoPosLSquare2;
        stereoPosRTriangle = masterVolume - stereoPosLTriangle;
        stereoPosRNoise = masterVolume - stereoPosLNoise;
        stereoPosRDMC = masterVolume - stereoPosLDMC;

    }

    public SourceDataLine getLine() {
        return line;
    }

    public boolean isRunning() {
        return (line != null && line.isActive());
    }

    public int getMillisToAvailableAbove(int target_avail) {

        double time;
        int cur_avail;
        if ((cur_avail = line.available()) >= target_avail) {
            return 0;
        }

        time = ((target_avail - cur_avail) * 1000) / sampleRate;
        time /= (stereo ? 4 : 2);

        return (int) time;

    }

    public int getBufferPos() {
        return bufferIndex;
    }

    public void initLengthLookup() {

        lengthLookup = new int[]{
                    0x0A, 0xFE,
                    0x14, 0x02,
                    0x28, 0x04,
                    0x50, 0x06,
                    0xA0, 0x08,
                    0x3C, 0x0A,
                    0x0E, 0x0C,
                    0x1A, 0x0E,
                    0x0C, 0x10,
                    0x18, 0x12,
                    0x30, 0x14,
                    0x60, 0x16,
                    0xC0, 0x18,
                    0x48, 0x1A,
                    0x10, 0x1C,
                    0x20, 0x1E
                };

    }

    public void initDmcFrequencyLookup() {

        dmcFreqLookup = new int[16];

        dmcFreqLookup[0x0] = 0xD60;
        dmcFreqLookup[0x1] = 0xBE0;
        dmcFreqLookup[0x2] = 0xAA0;
        dmcFreqLookup[0x3] = 0xA00;
        dmcFreqLookup[0x4] = 0x8F0;
        dmcFreqLookup[0x5] = 0x7F0;
        dmcFreqLookup[0x6] = 0x710;
        dmcFreqLookup[0x7] = 0x6B0;
        dmcFreqLookup[0x8] = 0x5F0;
        dmcFreqLookup[0x9] = 0x500;
        dmcFreqLookup[0xA] = 0x470;
        dmcFreqLookup[0xB] = 0x400;
        dmcFreqLookup[0xC] = 0x350;
        dmcFreqLookup[0xD] = 0x2A0;
        dmcFreqLookup[0xE] = 0x240;
        dmcFreqLookup[0xF] = 0x1B0;
    //for(int i=0;i<16;i++)dmcFreqLookup[i]/=8;

    }

    public void initNoiseWavelengthLookup() {

        noiseWavelengthLookup = new int[16];

        noiseWavelengthLookup[0x0] = 0x004;
        noiseWavelengthLookup[0x1] = 0x008;
        noiseWavelengthLookup[0x2] = 0x010;
        noiseWavelengthLookup[0x3] = 0x020;
        noiseWavelengthLookup[0x4] = 0x040;
        noiseWavelengthLookup[0x5] = 0x060;
        noiseWavelengthLookup[0x6] = 0x080;
        noiseWavelengthLookup[0x7] = 0x0A0;
        noiseWavelengthLookup[0x8] = 0x0CA;
        noiseWavelengthLookup[0x9] = 0x0FE;
        noiseWavelengthLookup[0xA] = 0x17C;
        noiseWavelengthLookup[0xB] = 0x1FC;
        noiseWavelengthLookup[0xC] = 0x2FA;
        noiseWavelengthLookup[0xD] = 0x3F8;
        noiseWavelengthLookup[0xE] = 0x7F2;
        noiseWavelengthLookup[0xF] = 0xFE4;

    }

    public void initDACtables() {

        square_table = new int[32 * 16];
        tnd_table = new int[204 * 16];
        double value;

        int ival;
        int max_sqr = 0;
        int max_tnd = 0;

        for (int i = 0; i < 32 * 16; i++) {


            value = 95.52 / (8128.0 / ((double) i / 16.0) + 100.0);
            value *= 0.98411;
            value *= 50000.0;
            ival = (int) value;

            square_table[i] = ival;
            if (ival > max_sqr) {
                max_sqr = ival;
            }

        }

        for (int i = 0; i < 204 * 16; i++) {

            value = 163.67 / (24329.0 / ((double) i / 16.0) + 100.0);
            value *= 0.98411;
            value *= 50000.0;
            ival = (int) value;

            tnd_table[i] = ival;
            if (ival > max_tnd) {
                max_tnd = ival;
            }

        }

        this.dacRange = max_sqr + max_tnd;
        this.dcValue = dacRange / 2;

    }

    public void destroy() {

        nes = null;
        cpuMem = null;

        if (square1 != null) {
            square1.destroy();
        }
        if (square2 != null) {
            square2.destroy();
        }
        if (triangle != null) {
            triangle.destroy();
        }
        if (noise != null) {
            noise.destroy();
        }
        if (dmc != null) {
            dmc.destroy();
        }

        square1 = null;
        square2 = null;
        triangle = null;
        ;
        noise = null;
        dmc = null;

        mixer = null;
        line = null;

    }
}
