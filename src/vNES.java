/*
vNES
Copyright © 2006-2013 Open Emulation Project

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

import java.applet.*;
import java.awt.*;

public class vNES extends Applet implements Runnable {

    boolean scale;
    boolean scanlines;
    boolean sound;
    boolean fps;
    boolean stereo;
    boolean timeemulation;
    boolean showsoundbuffer;
    int samplerate;
    int romSize;
    int progress;
    AppletUI gui;
    NES nes;
    ScreenView panelScreen;
    String rom = "";
    Font progressFont;
    Color bgColor = Color.black.darker().darker();
    boolean started = false;

    public void init() {
        initKeyCodes();
        readParams();
        System.gc();

        gui = new AppletUI(this);
        gui.init(false);

        Globals.appletMode = true;
        Globals.memoryFlushValue = 0x00; // make SMB1 hacked version work.

        nes = gui.getNES();
        nes.enableSound(sound);
        nes.reset();

    }

    public void addScreenView() {

        panelScreen = (ScreenView) gui.getScreenView();
        panelScreen.setFPSEnabled(fps);

        this.setLayout(null);

        if (scale) {

            if (scanlines) {
                panelScreen.setScaleMode(BufferView.SCALE_SCANLINE);
            } else {
                panelScreen.setScaleMode(BufferView.SCALE_NORMAL);
            }

            this.setSize(512, 480);
            this.setBounds(0, 0, 512, 480);
            panelScreen.setBounds(0, 0, 512, 480);

        } else {

            panelScreen.setBounds(0, 0, 256, 240);

        }

        this.setIgnoreRepaint(true);
        this.add(panelScreen);

    }

    public void start() {

        Thread t = new Thread(this);
        t.start();

    }

    public void run() {

        // Set font to be used for progress display of loading:
        progressFont = new Font("Tahoma", Font.TRUETYPE_FONT | Font.BOLD, 12);

        // Can start painting:
        started = true;

        // Load ROM file:
        System.out.println("vNES 2.16 \u00A9 2006-2013 Open Emulation Project");
        System.out.println("For updates, visit www.openemulation.com");
        System.out.println("Use of this program subject to GNU GPL, Version 3.");

        nes.loadRom(rom);

        if (nes.rom.isValid()) {

            // Add the screen buffer:
            addScreenView();

            // Set some properties:
            Globals.timeEmulation = timeemulation;
            nes.ppu.showSoundBuffer = showsoundbuffer;

            // Start emulation:
            //System.out.println("vNES is now starting the processor.");
            nes.getCpu().beginExecution();

        } else {

            // ROM file was invalid.
            System.out.println("vNES was unable to find (" + rom + ").");

        }

    }

    public void stop() {
        nes.stopEmulation();
        //System.out.println("vNES has stopped the processor.");
        nes.getPapu().stop();
        this.destroy();

    }

    public void destroy() {

        if (nes != null && nes.getCpu().isRunning()) {
            stop();
        }
        
        if (nes != null) {
            nes.destroy();
        }
        if (gui != null) {
            gui.destroy();
        }

        gui = null;
        nes = null;
        panelScreen = null;
        rom = null;

        System.runFinalization();
        System.gc();

    }

    public void showLoadProgress(int percentComplete) {

        progress = percentComplete;
        paint(getGraphics());

    }

    // Show the progress graphically.
    public void paint(Graphics g) {

        String pad;
        String disp;
        int scrw, scrh;
        int txtw, txth;

        if (!started) {
            return;
        }

        // Get screen size:
        if (scale) {
            scrw = 512;
            scrh = 480;
        } else {
            scrw = 256;
            scrh = 240;
        }

        // Fill background:
        g.setColor(bgColor);
        g.fillRect(0, 0, scrw, scrh);

        // Prepare text:
        if (progress < 10) {
            pad = "  ";
        } else if (progress < 100) {
            pad = " ";
        } else {
            pad = "";
        }
        disp = "vNES is Loading Game... " + pad + progress + "%";

        // Measure text:
        g.setFont(progressFont);
        txtw = g.getFontMetrics(progressFont).stringWidth(disp);
        txth = g.getFontMetrics(progressFont).getHeight();

        // Display text:
        g.setFont(progressFont);
        g.setColor(Color.white);
        g.drawString(disp, scrw / 2 - txtw / 2, scrh / 2 - txth / 2);
        g.drawString(disp, scrw / 2 - txtw / 2, scrh / 2 - txth / 2);
        g.drawString("vNES \u00A9 2006-2013 Open Emulation Project", 12, 464);
    }

    public void update(Graphics g) {
        // do nothing.
    }

    public void readParams() {

        String tmp;

        tmp = getParameter("rom");
        if (tmp == null || tmp.equals("")) {
            rom = "vnes.nes";
        } else {
            rom = tmp;
        }

        tmp = getParameter("scale");
        if (tmp == null || tmp.equals("")) {
            scale = false;
        } else {
            scale = tmp.equals("on");
        }

        tmp = getParameter("sound");
        if (tmp == null || tmp.equals("")) {
            sound = true;
        } else {
            sound = tmp.equals("on");
        }

        tmp = getParameter("stereo");
        if (tmp == null || tmp.equals("")) {
            stereo = true; // on by default
        } else {
            stereo = tmp.equals("on");
        }

        tmp = getParameter("scanlines");
        if (tmp == null || tmp.equals("")) {
            scanlines = false;
        } else {
            scanlines = tmp.equals("on");
        }

        tmp = getParameter("fps");
        if (tmp == null || tmp.equals("")) {
            fps = false;
        } else {
            fps = tmp.equals("on");
        }

        tmp = getParameter("timeemulation");
        if (tmp == null || tmp.equals("")) {
            timeemulation = true;
        } else {
            timeemulation = tmp.equals("on");
        }

        tmp = getParameter("showsoundbuffer");
        if (tmp == null || tmp.equals("")) {
            showsoundbuffer = false;
        } else {
            showsoundbuffer = tmp.equals("on");
        }

        /* Controller Setup for Player 1 */

        tmp = getParameter("p1_up");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p1_up", "VK_UP");
        } else {
            Globals.controls.put("p1_up", "VK_" + tmp);
        }
        tmp = getParameter("p1_down");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p1_down", "VK_DOWN");
        } else {
            Globals.controls.put("p1_down", "VK_" + tmp);
        }
        tmp = getParameter("p1_left");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p1_left", "VK_LEFT");
        } else {
            Globals.controls.put("p1_left", "VK_" + tmp);
        }
        tmp = getParameter("p1_right");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p1_right", "VK_RIGHT");
        } else {
            Globals.controls.put("p1_right", "VK_" + tmp);
        }
        tmp = getParameter("p1_a");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p1_a", "VK_X");
        } else {
            Globals.controls.put("p1_a", "VK_" + tmp);
        }
        tmp = getParameter("p1_b");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p1_b", "VK_Z");
        } else {
            Globals.controls.put("p1_b", "VK_" + tmp);
        }
        tmp = getParameter("p1_start");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p1_start", "VK_ENTER");
        } else {
            Globals.controls.put("p1_start", "VK_" + tmp);
        }
        tmp = getParameter("p1_select");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p1_select", "VK_CONTROL");
        } else {
            Globals.controls.put("p1_select", "VK_" + tmp);
        }

        /* Controller Setup for Player 2 */

        tmp = getParameter("p2_up");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p2_up", "VK_NUMPAD8");
        } else {
            Globals.controls.put("p2_up", "VK_" + tmp);
        }
        tmp = getParameter("p2_down");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p2_down", "VK_NUMPAD2");
        } else {
            Globals.controls.put("p2_down", "VK_" + tmp);
        }
        tmp = getParameter("p2_left");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p2_left", "VK_NUMPAD4");
        } else {
            Globals.controls.put("p2_left", "VK_" + tmp);
        }
        tmp = getParameter("p2_right");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p2_right", "VK_NUMPAD6");
        } else {
            Globals.controls.put("p2_right", "VK_" + tmp);
        }
        tmp = getParameter("p2_a");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p2_a", "VK_NUMPAD7");
        } else {
            Globals.controls.put("p2_a", "VK_" + tmp);
        }
        tmp = getParameter("p2_b");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p2_b", "VK_NUMPAD9");
        } else {
            Globals.controls.put("p2_b", "VK_" + tmp);
        }
        tmp = getParameter("p2_start");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p2_start", "VK_NUMPAD1");
        } else {
            Globals.controls.put("p2_start", "VK_" + tmp);
        }
        tmp = getParameter("p2_select");
        if (tmp == null || tmp.equals("")) {
            Globals.controls.put("p2_select", "VK_NUMPAD3");
        } else {
            Globals.controls.put("p2_select", "VK_" + tmp);
        }

        tmp = getParameter("romsize");
        if (tmp == null || tmp.equals("")) {
            romSize = -1;
        } else {
            try {
                romSize = Integer.parseInt(tmp);
            } catch (Exception e) {
                romSize = -1;
            }
        }
    }

    public void initKeyCodes() {
        Globals.keycodes.put("VK_SPACE", 32);
        Globals.keycodes.put("VK_PAGE_UP", 33);
        Globals.keycodes.put("VK_PAGE_DOWN", 34);
        Globals.keycodes.put("VK_END", 35);
        Globals.keycodes.put("VK_HOME", 36);
        Globals.keycodes.put("VK_DELETE", 127);
        Globals.keycodes.put("VK_INSERT", 155);
        Globals.keycodes.put("VK_LEFT", 37);
        Globals.keycodes.put("VK_UP", 38);
        Globals.keycodes.put("VK_RIGHT", 39);
        Globals.keycodes.put("VK_DOWN", 40);
        Globals.keycodes.put("VK_0", 48);
        Globals.keycodes.put("VK_1", 49);
        Globals.keycodes.put("VK_2", 50);
        Globals.keycodes.put("VK_3", 51);
        Globals.keycodes.put("VK_4", 52);
        Globals.keycodes.put("VK_5", 53);
        Globals.keycodes.put("VK_6", 54);
        Globals.keycodes.put("VK_7", 55);
        Globals.keycodes.put("VK_8", 56);
        Globals.keycodes.put("VK_9", 57);
        Globals.keycodes.put("VK_A", 65);
        Globals.keycodes.put("VK_B", 66);
        Globals.keycodes.put("VK_C", 67);
        Globals.keycodes.put("VK_D", 68);
        Globals.keycodes.put("VK_E", 69);
        Globals.keycodes.put("VK_F", 70);
        Globals.keycodes.put("VK_G", 71);
        Globals.keycodes.put("VK_H", 72);
        Globals.keycodes.put("VK_I", 73);
        Globals.keycodes.put("VK_J", 74);
        Globals.keycodes.put("VK_K", 75);
        Globals.keycodes.put("VK_L", 76);
        Globals.keycodes.put("VK_M", 77);
        Globals.keycodes.put("VK_N", 78);
        Globals.keycodes.put("VK_O", 79);
        Globals.keycodes.put("VK_P", 80);
        Globals.keycodes.put("VK_Q", 81);
        Globals.keycodes.put("VK_R", 82);
        Globals.keycodes.put("VK_S", 83);
        Globals.keycodes.put("VK_T", 84);
        Globals.keycodes.put("VK_U", 85);
        Globals.keycodes.put("VK_V", 86);
        Globals.keycodes.put("VK_W", 87);
        Globals.keycodes.put("VK_X", 88);
        Globals.keycodes.put("VK_Y", 89);
        Globals.keycodes.put("VK_Z", 90);
        Globals.keycodes.put("VK_NUMPAD0", 96);
        Globals.keycodes.put("VK_NUMPAD1", 97);
        Globals.keycodes.put("VK_NUMPAD2", 98);
        Globals.keycodes.put("VK_NUMPAD3", 99);
        Globals.keycodes.put("VK_NUMPAD4", 100);
        Globals.keycodes.put("VK_NUMPAD5", 101);
        Globals.keycodes.put("VK_NUMPAD6", 102);
        Globals.keycodes.put("VK_NUMPAD7", 103);
        Globals.keycodes.put("VK_NUMPAD8", 104);
        Globals.keycodes.put("VK_NUMPAD9", 105);
        Globals.keycodes.put("VK_MULTIPLY", 106);
        Globals.keycodes.put("VK_ADD", 107);
        Globals.keycodes.put("VK_SUBTRACT", 109);
        Globals.keycodes.put("VK_DECIMAL", 110);
        Globals.keycodes.put("VK_DIVIDE", 111);
        Globals.keycodes.put("VK_BACK_SPACE", 8);
        Globals.keycodes.put("VK_TAB", 9);
        Globals.keycodes.put("VK_ENTER", 10);
        Globals.keycodes.put("VK_SHIFT", 16);
        Globals.keycodes.put("VK_CONTROL", 17);
        Globals.keycodes.put("VK_ALT", 18);
        Globals.keycodes.put("VK_PAUSE", 19);
        Globals.keycodes.put("VK_ESCAPE", 27);
        Globals.keycodes.put("VK_OPEN_BRACKET", 91);
        Globals.keycodes.put("VK_BACK_SLASH", 92);
        Globals.keycodes.put("VK_CLOSE_BRACKET", 93);
        Globals.keycodes.put("VK_SEMICOLON", 59);
        Globals.keycodes.put("VK_QUOTE", 222);
        Globals.keycodes.put("VK_COMMA", 44);
        Globals.keycodes.put("VK_MINUS", 45);
        Globals.keycodes.put("VK_PERIOD", 46);
        Globals.keycodes.put("VK_SLASH", 47);
    }
}
