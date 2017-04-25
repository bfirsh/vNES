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

import java.awt.event.*;

public class ScreenView extends BufferView {

    private MyMouseAdapter mouse;
    private boolean notifyImageReady;

    public ScreenView(NES nes, int width, int height) {
        super(nes, width, height);
    }

    public void init() {

        if (mouse == null) {
            mouse = new MyMouseAdapter();
            this.addMouseListener(mouse);
        }
        super.init();

    }

    private class MyMouseAdapter extends MouseAdapter {

        long lastClickTime = 0;

        public void mouseClicked(MouseEvent me) {
            setFocusable(true);
            requestFocus();
        }

        public void mousePressed(MouseEvent me) {
            setFocusable(true);
            requestFocus();

            if (me.getX() >= 0 && me.getY() >= 0 && me.getX() < 256 && me.getY() < 240) {
                if (nes != null && nes.memMapper != null) {
                    nes.memMapper.setMouseState(true, me.getX(), me.getY());
                }
            }

        }

        public void mouseReleased(MouseEvent me) {

            if (nes != null && nes.memMapper != null) {
                nes.memMapper.setMouseState(false, 0, 0);
            }

        }
    }

    public void setNotifyImageReady(boolean value) {
        this.notifyImageReady = value;
    }

    public void imageReady(boolean skipFrame) {

        if (!Globals.focused) {
            setFocusable(true);
            requestFocus();
            Globals.focused = true;
        }

        // Draw image first:
        super.imageReady(skipFrame);

        // Notify GUI, so it can write the sound buffer:
        if (notifyImageReady) {
            nes.getGui().imageReady(skipFrame);
        }

    }
}