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

public class AppletUI implements UI {

    vNES applet;
    NES nes;
    KbInputHandler kbJoy1;
    KbInputHandler kbJoy2;
    ScreenView vScreen;
    HiResTimer timer;
    long t1, t2;
    int sleepTime;

    public AppletUI(vNES applet) {

        timer = new HiResTimer();
        this.applet = applet;
        nes = new NES(this);
    }

    public void init(boolean showGui) {

        vScreen = new ScreenView(nes, 256, 240);
        vScreen.setBgColor(applet.bgColor.getRGB());
        vScreen.init();
        vScreen.setNotifyImageReady(true);

        kbJoy1 = new KbInputHandler(nes, 0);
        kbJoy2 = new KbInputHandler(nes, 1);

        // Grab Controller Setting for Player 1:
        kbJoy1.mapKey(InputHandler.KEY_A, (Integer) Globals.keycodes.get(Globals.controls.get("p1_a")));
        kbJoy1.mapKey(InputHandler.KEY_B, (Integer) Globals.keycodes.get(Globals.controls.get("p1_b")));
        kbJoy1.mapKey(InputHandler.KEY_START, (Integer) Globals.keycodes.get(Globals.controls.get("p1_start")));
        kbJoy1.mapKey(InputHandler.KEY_SELECT, (Integer) Globals.keycodes.get(Globals.controls.get("p1_select")));
        kbJoy1.mapKey(InputHandler.KEY_UP, (Integer) Globals.keycodes.get(Globals.controls.get("p1_up")));
        kbJoy1.mapKey(InputHandler.KEY_DOWN, (Integer) Globals.keycodes.get(Globals.controls.get("p1_down")));
        kbJoy1.mapKey(InputHandler.KEY_LEFT, (Integer) Globals.keycodes.get(Globals.controls.get("p1_left")));
        kbJoy1.mapKey(InputHandler.KEY_RIGHT, (Integer) Globals.keycodes.get(Globals.controls.get("p1_right")));
        vScreen.addKeyListener(kbJoy1);

        // Grab Controller Setting for Player 2:
        kbJoy2.mapKey(InputHandler.KEY_A, (Integer) Globals.keycodes.get(Globals.controls.get("p2_a")));
        kbJoy2.mapKey(InputHandler.KEY_B, (Integer) Globals.keycodes.get(Globals.controls.get("p2_b")));
        kbJoy2.mapKey(InputHandler.KEY_START, (Integer) Globals.keycodes.get(Globals.controls.get("p2_start")));
        kbJoy2.mapKey(InputHandler.KEY_SELECT, (Integer) Globals.keycodes.get(Globals.controls.get("p2_select")));
        kbJoy2.mapKey(InputHandler.KEY_UP, (Integer) Globals.keycodes.get(Globals.controls.get("p2_up")));
        kbJoy2.mapKey(InputHandler.KEY_DOWN, (Integer) Globals.keycodes.get(Globals.controls.get("p2_down")));
        kbJoy2.mapKey(InputHandler.KEY_LEFT, (Integer) Globals.keycodes.get(Globals.controls.get("p2_left")));
        kbJoy2.mapKey(InputHandler.KEY_RIGHT, (Integer) Globals.keycodes.get(Globals.controls.get("p2_right")));
        vScreen.addKeyListener(kbJoy2);
    }

    public void imageReady(boolean skipFrame) {

        // Sound stuff:
        int tmp = nes.getPapu().bufferIndex;
        if (Globals.enableSound && Globals.timeEmulation && tmp > 0) {

            int min_avail = nes.getPapu().line.getBufferSize() - 4 * tmp;

            long timeToSleep = nes.papu.getMillisToAvailableAbove(min_avail);
            do {
                try {
                    Thread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                }
            } while ((timeToSleep = nes.papu.getMillisToAvailableAbove(min_avail)) > 0);

            nes.getPapu().writeBuffer();

        }

        // Sleep a bit if sound is disabled:
        if (Globals.timeEmulation && !Globals.enableSound) {

            sleepTime = Globals.frameTime;
            if ((t2 = timer.currentMicros()) - t1 < sleepTime) {
                timer.sleepMicros(sleepTime - (t2 - t1));
            }

        }

        // Update timer:
        t1 = t2;

    }

    public int getRomFileSize() {
        return applet.romSize;
    }

    public void showLoadProgress(int percentComplete) {

        // Show ROM load progress:
        applet.showLoadProgress(percentComplete);

        // Sleep a bit:
        timer.sleepMicros(20 * 1000);

    }

    public void destroy() {

        if (vScreen != null) {
            vScreen.destroy();
        }
        if (kbJoy1 != null) {
            kbJoy1.destroy();
        }
        if (kbJoy2 != null) {
            kbJoy2.destroy();
        }

        nes = null;
        applet = null;
        kbJoy1 = null;
        kbJoy2 = null;
        vScreen = null;
        timer = null;

    }

    public NES getNES() {
        return nes;
    }

    public InputHandler getJoy1() {
        return kbJoy1;
    }

    public InputHandler getJoy2() {
        return kbJoy2;
    }

    public BufferView getScreenView() {
        return vScreen;
    }

    public BufferView getPatternView() {
        return null;
    }

    public BufferView getSprPalView() {
        return null;
    }

    public BufferView getNameTableView() {
        return null;
    }

    public BufferView getImgPalView() {
        return null;
    }

    public HiResTimer getTimer() {
        return timer;
    }

    public String getWindowCaption() {
        return "";
    }

    public void setWindowCaption(String s) {
    }

    public void setTitle(String s) {
    }

    public java.awt.Point getLocation() {
        return new java.awt.Point(0, 0);
    }

    public int getWidth() {
        return applet.getWidth();
    }

    public int getHeight() {
        return applet.getHeight();
    }

    public void println(String s) {
    }

    public void showErrorMsg(String msg) {
        System.out.println(msg);
    }
}