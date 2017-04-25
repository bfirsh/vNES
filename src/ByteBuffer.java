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

import java.io.*;
import java.util.zip.*;

public class ByteBuffer {

    public static final boolean DEBUG = false;
    public static final int BO_BIG_ENDIAN = 0;
    public static final int BO_LITTLE_ENDIAN = 1;
    private int byteOrder = BO_BIG_ENDIAN;
    private short[] buf;
    private int size;
    private int curPos;
    private boolean hasBeenErrors;
    private boolean expandable = true;
    private int expandBy = 4096;

    public ByteBuffer(int size, int byteOrdering) {
        if (size < 1) {
            size = 1;
        }
        buf = new short[size];
        this.size = size;
        this.byteOrder = byteOrdering;
        curPos = 0;
        hasBeenErrors = false;
    }

    public ByteBuffer(byte[] content, int byteOrdering) {
        try {
            buf = new short[content.length];
            for (int i = 0; i < content.length; i++) {
                buf[i] = (short) (content[i] & 255);
            }
            size = content.length;
            this.byteOrder = byteOrdering;
            curPos = 0;
            hasBeenErrors = false;
        } catch (Exception e) {
            //System.out.println("ByteBuffer: Couldn't create buffer from empty array.");
        }
    }

    public void setExpandable(boolean exp) {
        expandable = exp;
    }

    public void setExpandBy(int expBy) {

        if (expBy > 1024) {
            this.expandBy = expBy;
        }

    }

    public void setByteOrder(int byteOrder) {

        if (byteOrder >= 0 && byteOrder < 2) {
            this.byteOrder = byteOrder;
        }

    }

    public byte[] getBytes() {
        byte[] ret = new byte[buf.length];
        for (int i = 0; i < buf.length; i++) {
            ret[i] = (byte) buf[i];
        }
        return ret;
    }

    public int getSize() {
        return this.size;
    }

    public int getPos() {
        return curPos;
    }

    private void error() {
        hasBeenErrors = true;
    //System.out.println("Not in range!");
    }

    public boolean hasHadErrors() {
        return hasBeenErrors;
    }

    public void clear() {
        for (int i = 0; i < buf.length; i++) {
            buf[i] = 0;
        }
        curPos = 0;
    }

    public void fill(byte value) {
        for (int i = 0; i < size; i++) {
            buf[i] = value;
        }
    }

    public boolean fillRange(int start, int length, byte value) {
        if (inRange(start, length)) {
            for (int i = start; i < (start + length); i++) {
                buf[i] = value;
            }
            return true;
        } else {
            error();
            return false;
        }
    }

    public void resize(int length) {

        short[] newbuf = new short[length];
        System.arraycopy(buf, 0, newbuf, 0, Math.min(length, size));
        buf = newbuf;
        size = length;

    }

    public void resizeToCurrentPos() {
        resize(curPos);
    }

    public void expand() {
        expand(expandBy);
    }

    public void expand(int byHowMuch) {
        resize(size + byHowMuch);
    }

    public void goTo(int position) {
        if (inRange(position)) {
            curPos = position;
        } else {
            error();
        }
    }

    public void move(int howFar) {
        curPos += howFar;
        if (!inRange(curPos)) {
            curPos = size - 1;
        }
    }

    public boolean inRange(int pos) {
        if (pos >= 0 && pos < size) {
            return true;
        } else {
            if (expandable) {
                expand(Math.max(pos + 1 - size, expandBy));
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean inRange(int pos, int length) {
        if (pos >= 0 && pos + (length - 1) < size) {
            return true;
        } else {
            if (expandable) {
                expand(Math.max(pos + length - size, expandBy));
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean putBoolean(boolean b) {
        boolean ret = putBoolean(b, curPos);
        move(1);
        return ret;
    }

    public boolean putBoolean(boolean b, int pos) {
        if (b) {
            return putByte((short) 1, pos);
        } else {
            return putByte((short) 0, pos);
        }
    }

    public boolean putByte(short var) {
        if (inRange(curPos, 1)) {
            buf[curPos] = var;
            move(1);
            return true;
        } else {
            error();
            return false;
        }
    }

    public boolean putByte(short var, int pos) {
        if (inRange(pos, 1)) {
            buf[pos] = var;
            return true;
        } else {
            error();
            return false;
        }
    }

    public boolean putShort(short var) {
        boolean ret = putShort(var, curPos);
        if (ret) {
            move(2);
        }
        return ret;
    }

    public boolean putShort(short var, int pos) {
        if (inRange(pos, 2)) {
            if (this.byteOrder == BO_BIG_ENDIAN) {
                buf[pos + 0] = (short) ((var >> 8) & 255);
                buf[pos + 1] = (short) ((var) & 255);
            } else {
                buf[pos + 1] = (short) ((var >> 8) & 255);
                buf[pos + 0] = (short) ((var) & 255);
            }
            return true;
        } else {
            error();
            return false;
        }
    }

    public boolean putInt(int var) {
        boolean ret = putInt(var, curPos);
        if (ret) {
            move(4);
        }
        return ret;
    }

    public boolean putInt(int var, int pos) {
        if (inRange(pos, 4)) {
            if (this.byteOrder == BO_BIG_ENDIAN) {
                buf[pos + 0] = (short) ((var >> 24) & 255);
                buf[pos + 1] = (short) ((var >> 16) & 255);
                buf[pos + 2] = (short) ((var >> 8) & 255);
                buf[pos + 3] = (short) ((var) & 255);
            } else {
                buf[pos + 3] = (short) ((var >> 24) & 255);
                buf[pos + 2] = (short) ((var >> 16) & 255);
                buf[pos + 1] = (short) ((var >> 8) & 255);
                buf[pos + 0] = (short) ((var) & 255);
            }
            return true;
        } else {
            error();
            return false;
        }
    }

    public boolean putString(String var) {
        boolean ret = putString(var, curPos);
        if (ret) {
            move(2 * var.length());
        }
        return ret;
    }

    public boolean putString(String var, int pos) {
        char[] charArr = var.toCharArray();
        short theChar;
        if (inRange(pos, var.length() * 2)) {
            for (int i = 0; i < var.length(); i++) {
                theChar = (short) (charArr[i]);
                buf[pos + 0] = (short) ((theChar >> 8) & 255);
                buf[pos + 1] = (short) ((theChar) & 255);
                pos += 2;
            }
            return true;
        } else {
            error();
            return false;
        }
    }

    public boolean putChar(char var) {
        boolean ret = putChar(var, curPos);
        if (ret) {
            move(2);
        }
        return ret;
    }

    public boolean putChar(char var, int pos) {
        int tmp = var;
        if (inRange(pos, 2)) {
            if (byteOrder == BO_BIG_ENDIAN) {
                buf[pos + 0] = (short) ((tmp >> 8) & 255);
                buf[pos + 1] = (short) ((tmp) & 255);
            } else {
                buf[pos + 1] = (short) ((tmp >> 8) & 255);
                buf[pos + 0] = (short) ((tmp) & 255);
            }
            return true;
        } else {
            error();
            return false;
        }
    }

    public boolean putCharAscii(char var) {
        boolean ret = putCharAscii(var, curPos);
        if (ret) {
            move(1);
        }
        return ret;
    }

    public boolean putCharAscii(char var, int pos) {
        if (inRange(pos)) {
            buf[pos] = (short) var;
            return true;
        } else {
            error();
            return false;
        }
    }

    public boolean putStringAscii(String var) {
        boolean ret = putStringAscii(var, curPos);
        if (ret) {
            move(var.length());
        }
        return ret;
    }

    public boolean putStringAscii(String var, int pos) {
        char[] charArr = var.toCharArray();
        if (inRange(pos, var.length())) {
            for (int i = 0; i < var.length(); i++) {
                buf[pos] = (short) charArr[i];
                pos++;
            }
            return true;
        } else {
            error();
            return false;
        }
    }

    public boolean putByteArray(short[] arr) {
        if (arr == null) {
            return false;
        }
        if (buf.length - curPos < arr.length) {
            resize(curPos + arr.length);
        }
        for (int i = 0; i < arr.length; i++) {
            buf[curPos + i] = (byte) arr[i];
        }
        curPos += arr.length;
        return true;
    }

    public boolean readByteArray(short[] arr) {
        if (arr == null) {
            return false;
        }
        if (buf.length - curPos < arr.length) {
            return false;
        }
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (short) (buf[curPos + i] & 0xFF);
        }
        curPos += arr.length;
        return true;
    }

    public boolean putShortArray(short[] arr) {
        if (arr == null) {
            return false;
        }
        if (buf.length - curPos < arr.length * 2) {
            resize(curPos + arr.length * 2);
        }
        if (byteOrder == BO_BIG_ENDIAN) {
            for (int i = 0; i < arr.length; i++) {
                buf[curPos + 0] = (short) ((arr[i] >> 8) & 255);
                buf[curPos + 1] = (short) ((arr[i]) & 255);
                curPos += 2;
            }
        } else {
            for (int i = 0; i < arr.length; i++) {
                buf[curPos + 1] = (short) ((arr[i] >> 8) & 255);
                buf[curPos + 0] = (short) ((arr[i]) & 255);
                curPos += 2;
            }
        }
        return true;
    }

    public String toString() {
        StringBuffer strBuf = new StringBuffer();
        short tmp;
        for (int i = 0; i < (size - 1); i += 2) {
            tmp = (short) ((buf[i] << 8) | (buf[i + 1]));
            strBuf.append((char) (tmp));
        }
        return strBuf.toString();
    }

    public String toStringAscii() {
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < size; i++) {
            strBuf.append((char) (buf[i]));
        }
        return strBuf.toString();
    }

    public boolean readBoolean() {
        boolean ret = readBoolean(curPos);
        move(1);
        return ret;
    }

    public boolean readBoolean(int pos) {
        return readByte(pos) == 1;
    }

    public short readByte() throws ArrayIndexOutOfBoundsException {
        short ret = readByte(curPos);
        move(1);
        return ret;
    }

    public short readByte(int pos) throws ArrayIndexOutOfBoundsException {
        if (inRange(pos)) {
            return buf[pos];
        } else {
            error();
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public short readShort() throws ArrayIndexOutOfBoundsException {
        short ret = readShort(curPos);
        move(2);
        return ret;
    }

    public short readShort(int pos) throws ArrayIndexOutOfBoundsException {
        if (inRange(pos, 2)) {
            if (this.byteOrder == BO_BIG_ENDIAN) {
                return (short) ((buf[pos] << 8) | (buf[pos + 1]));
            } else {
                return (short) ((buf[pos + 1] << 8) | (buf[pos]));
            }
        } else {
            error();
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public int readInt() throws ArrayIndexOutOfBoundsException {
        int ret = readInt(curPos);
        move(4);
        return ret;
    }

    public int readInt(int pos) throws ArrayIndexOutOfBoundsException {
        int ret = 0;
        if (inRange(pos, 4)) {
            if (this.byteOrder == BO_BIG_ENDIAN) {
                ret |= (buf[pos + 0] << 24);
                ret |= (buf[pos + 1] << 16);
                ret |= (buf[pos + 2] << 8);
                ret |= (buf[pos + 3]);
            } else {
                ret |= (buf[pos + 3] << 24);
                ret |= (buf[pos + 2] << 16);
                ret |= (buf[pos + 1] << 8);
                ret |= (buf[pos + 0]);
            }
            return ret;
        } else {
            error();
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public char readChar() throws ArrayIndexOutOfBoundsException {
        char ret = readChar(curPos);
        move(2);
        return ret;
    }

    public char readChar(int pos) throws ArrayIndexOutOfBoundsException {
        if (inRange(pos, 2)) {
            return (char) (readShort(pos));
        } else {
            error();
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public char readCharAscii() throws ArrayIndexOutOfBoundsException {
        char ret = readCharAscii(curPos);
        move(1);
        return ret;
    }

    public char readCharAscii(int pos) throws ArrayIndexOutOfBoundsException {
        if (inRange(pos, 1)) {
            return (char) (readByte(pos) & 255);
        } else {
            error();
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public String readString(int length) throws ArrayIndexOutOfBoundsException {
        if (length > 0) {
            String ret = readString(curPos, length);
            move(ret.length() * 2);
            return ret;
        } else {
            return new String("");
        }
    }

    public String readString(int pos, int length) throws ArrayIndexOutOfBoundsException {
        char[] tmp;
        if (inRange(pos, length * 2) && length > 0) {
            tmp = new char[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = readChar(pos + i * 2);
            }
            return new String(tmp);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public String readStringWithShortLength() throws ArrayIndexOutOfBoundsException {
        String ret = readStringWithShortLength(curPos);
        move(ret.length() * 2 + 2);
        return ret;
    }

    public String readStringWithShortLength(int pos) throws ArrayIndexOutOfBoundsException {
        short len;
        if (inRange(pos, 2)) {
            len = readShort(pos);
            if (len > 0) {
                return readString(pos + 2, len);
            } else {
                return new String("");
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public String readStringAscii(int length) throws ArrayIndexOutOfBoundsException {
        String ret = readStringAscii(curPos, length);
        move(ret.length());
        return ret;
    }

    public String readStringAscii(int pos, int length) throws ArrayIndexOutOfBoundsException {
        char[] tmp;
        if (inRange(pos, length) && length > 0) {
            tmp = new char[length];
            for (int i = 0; i < length; i++) {
                tmp[i] = readCharAscii(pos + i);
            }
            return new String(tmp);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    public String readStringAsciiWithShortLength() throws ArrayIndexOutOfBoundsException {
        String ret = readStringAsciiWithShortLength(curPos);
        move(ret.length() + 2);
        return ret;
    }

    public String readStringAsciiWithShortLength(int pos) throws ArrayIndexOutOfBoundsException {
        short len;
        if (inRange(pos, 2)) {
            len = readShort(pos);
            if (len > 0) {
                return readStringAscii(pos + 2, len);
            } else {
                return new String("");
            }
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    private short[] expandShortArray(short[] array, int size) {
        short[] newArr = new short[array.length + size];
        if (size > 0) {
            System.arraycopy(array, 0, newArr, 0, array.length);
        } else {
            System.arraycopy(array, 0, newArr, 0, newArr.length);
        }
        return newArr;
    }

    public void crop() {
        if (curPos > 0) {
            if (curPos < buf.length) {
                short[] newBuf = new short[curPos];
                System.arraycopy(buf, 0, newBuf, 0, curPos);
                buf = newBuf;
            }
        } else {
            //System.out.println("Could not crop buffer, as the current position is 0. The buffer may not be empty.");
        }
    }

    public static ByteBuffer asciiEncode(ByteBuffer buf) {

        short[] data = buf.buf;
        byte[] enc = new byte[buf.getSize() * 2];

        int encpos = 0;
        int tmp;
        for (int i = 0; i < data.length; i++) {

            tmp = data[i];
            enc[encpos] = (byte) (65 + (tmp) & 0xF);
            enc[encpos + 1] = (byte) (65 + (tmp >> 4) & 0xF);
            encpos += 2;

        }
        return new ByteBuffer(enc, ByteBuffer.BO_BIG_ENDIAN);

    }

    public static ByteBuffer asciiDecode(ByteBuffer buf) {
        return null;
    }

    public static void saveToZipFile(File f, ByteBuffer buf) {

        try {

            FileOutputStream fOut = new FileOutputStream(f);
            ZipOutputStream zipOut = new ZipOutputStream(fOut);
            zipOut.putNextEntry(new ZipEntry("contents"));
            zipOut.write(buf.getBytes());
            zipOut.closeEntry();
            zipOut.close();
            fOut.close();
        //System.out.println("Buffer was successfully saved to "+f.getPath());

        } catch (Exception e) {

            //System.out.println("Unable to save buffer to file "+f.getPath());
            e.printStackTrace();

        }

    }

    public static ByteBuffer readFromZipFile(File f) {

        try {

            FileInputStream in = new FileInputStream(f);
            ZipInputStream zipIn = new ZipInputStream(in);
            int len, curlen, read;

            ZipFile zip = new ZipFile(f);
            ZipEntry entry = zip.getEntry("contents");
            len = (int) entry.getSize();
            //System.out.println("Len = "+len);

            curlen = 0;
            byte[] buf = new byte[len];
            zipIn.getNextEntry();
            while (curlen < len) {
                read = zipIn.read(buf, curlen, len - curlen);
                if (read >= 0) {
                    curlen += read;
                } else {
                    // end of file.
                    break;
                }
            }
            zipIn.closeEntry();
            zipIn.close();
            in.close();
            zip.close();
            return new ByteBuffer(buf, ByteBuffer.BO_BIG_ENDIAN);

        } catch (Exception e) {
            //System.out.println("Unable to load buffer from file "+f.getPath());
            e.printStackTrace();
        }

        // fail:
        return null;

    }
}