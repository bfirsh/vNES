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

import java.awt.*;

public interface UI {

    public NES getNES();

    public InputHandler getJoy1();

    public InputHandler getJoy2();

    public BufferView getScreenView();

    public BufferView getPatternView();

    public BufferView getSprPalView();

    public BufferView getNameTableView();

    public BufferView getImgPalView();

    public HiResTimer getTimer();

    public void imageReady(boolean skipFrame);

    public void init(boolean showGui);

    public String getWindowCaption();

    public void setWindowCaption(String s);

    public void setTitle(String s);

    public Point getLocation();

    public int getWidth();

    public int getHeight();

    public int getRomFileSize();

    public void destroy();

    public void println(String s);

    public void showLoadProgress(int percentComplete);

    public void showErrorMsg(String msg);
}