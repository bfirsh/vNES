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

import java.util.*;

public class GameGenie {

    // 6 char codes:
    //
    // Char # |   1   |   2   |   3   |   4   |   5   |   6   |
    // Bit  # |3|2|1|0|3|2|1|0|3|2|1|0|3|2|1|0|3|2|1|0|3|2|1|0|
    // maps to|1|6|7|8|H|2|3|4|-|I|J|K|L|A|B|C|D|M|N|O|5|E|F|G|
    //
    // 12345678        -> value
    // ABCDEFGHIJKLMNO -> address
    // 8 char codes:
    //
    //	Char # |   1   |   2   |   3   |   4   |   5   |   6   |   7   |   8   |
    //	Bit  # |3|2|1|0|3|2|1|0|3|2|1|0|3|2|1|0|3|2|1|0|3|2|1|0|3|2|1|0|3|2|1|0|
    //	maps to|1|6|7|8|H|2|3|4|-|I|J|K|L|A|B|C|D|M|N|O|%|E|F|G|!|^|&|*|5|@|#|$|
    //
    // 12345678        -> value
    // ABCDEFGHIJKLMNO -> address
    // !@#$%^&*        -> compare value

    // Code types:
    public static int TYPE_6CHAR = 0;
    public static int TYPE_8CHAR = 1;
    private ArrayList codeList;
    public boolean[] addressMatch;

    // character mapping:
    private String[] charMapping = new String[]{
        "a", "p", "z", "l", "g", "i", "t", "y", "e", "o", "x", "u", "k", "s", "v", "n"
    };

    // 6 char code descrambling lookup table:
    private int[] lut6charMapping = new int[]{
        20, 21, 22, 3, 16, 17, 18, 23,
        4, 5, 6, 11, 12, 13, 14, 19, 0, 1, 2, 7, 8, 9, 10
    };

    // 8 char code descrambling lookup table:
    private int[] lut8charMapping = new int[]{
        28, 29, 30, 3, 24, 25, 26, 31,
        12, 13, 14, 19, 20, 21, 22, 27, 8, 9, 10, 15, 16, 17, 18,
        4, 5, 6, 11, 0, 1, 2, 7
    };

    public GameGenie() {

        addressMatch = new boolean[0x10000];
        codeList = new ArrayList();

    }

    public void clearCodes() {

        codeList.clear();
        updateAddressMatch();

    }

    public void updateAddressMatch() {

        for (int i = 0; i < addressMatch.length; i++) {
            addressMatch[i] = false;
        }
        for (int i = 0; i < codeList.size(); i++) {
            addressMatch[((GameGenieCode) codeList.get(i)).address] = true;
        }

    }

    public void addCode(String code) {

        if (code.length() == 6 || code.length() == 8) {

            GameGenieCode newCode = new GameGenieCode();
            codeList.add(newCode);
            newCode.type = (code.length() == 6 ? TYPE_6CHAR : TYPE_8CHAR);
            newCode.code = code;

            if (newCode.type == TYPE_6CHAR) {

                newCode.address = get6charAddress(code);
                newCode.value = get6charValue(code);

            } else {

                newCode.address = get8charAddress(code);
                newCode.value = get8charValue(code);
                newCode.compare = get8charCompare(code);

            }

            addressMatch[newCode.address] = true;

        } else {
            //System.out.println("Invalid code length. Code: "+code);
        }

    }

    public void editCode(int codeindex, String newCode) {

        if (newCode.length() == 6 || newCode.length() == 8) {
            GameGenieCode code = (GameGenieCode) codeList.get(codeindex);
            code.code = newCode;
            code.type = (newCode.length() == 6 ? TYPE_6CHAR : TYPE_8CHAR);
            if (code.type == TYPE_6CHAR) {
                code.address = get6charAddress(newCode);
                code.value = get6charValue(newCode);
            } else {
                code.address = get8charAddress(newCode);
                code.value = get8charValue(newCode);
                code.compare = get8charCompare(newCode);
            }
            updateAddressMatch();
        }

    }

    public void removeCode(int codeindex) {
        codeList.remove(codeindex);
        updateAddressMatch();
    }

    public int getCodeIndex(int address) {
        for (int i = 0; i < codeList.size(); i++) {
            if (getCode(i).address == address) {
                return i;
            }
        }
        //System.out.println("Something's wrong. Couldn't find game genie code index. address="+Misc.hex16(address));
        return 0;
    }

    public int getCodeCount() {
        return codeList.size();
    }

    public GameGenieCode getCode(int index) {
        return (GameGenieCode) codeList.get(index);
    }

    public int getCodeType(int index) {
        return getCode(index).type;
    }

    public String getCodeString(int index) {
        return getCode(index).code;
    }

    public int getCodeAddress(int index) {
        return getCode(index).address;
    }

    public int getCodeValue(int index) {
        return getCode(index).value;
    }

    public int getCodeCompare(int index) {
        return getCode(index).compare;
    }

    // Decodes a 6 char GG code so that the address and such may be extracted.
    public long decode6char(String code) {

        long scrambled = mapChars(code);
        long descrambled = 0;
        for (int i = 0; i < 23; i++) {
            descrambled |= (((scrambled >> lut6charMapping[i]) & 1) << i);
        }
        return descrambled;
    }

    // Decodes an 8 char GG code so that the address and such may be extracted.
    public long decode8char(String code) {

        long scrambled = mapChars(code);
        long descrambled = 0;
        for (int i = 0; i < 31; i++) {
            descrambled |= (((scrambled >> lut8charMapping[i]) & 1) << i);
        }
        return descrambled;
    }

    // Decodes the chars into a number.
    public long mapChars(String s) {

        long res = 0;
        s = s.toLowerCase();
        for (int i = 0; i < s.length(); i++) {
            res |= (getCharNum(s.substring(i, i + 1)) << ((s.length() - i - 1) * 4));
        }
        return res;
    }

    // Returns the mapped character number.
    public int getCharNum(String s) {

        for (int i = 0; i < charMapping.length; i++) {
            if (s.equals(charMapping[i])) {
                return i;
            }
        }
        return 0;
    }

    // Return the address from a 6 char code.
    public int get6charAddress(String code) {
        return (int) ((decode6char(code) & 0xFFFF00) >> 8) | 0x8000;
    }

    // Return the value from a 6 char code.
    public int get6charValue(String code) {
        return (int) (decode6char(code) & 0x00000FF);
    }

    // Return the address from an 8 char code.
    public int get8charAddress(String code) {
        return (int) ((decode8char(code) >> 8) & (65535 - 32768)) | 0x8000;
    }

    // Return the value from an 8 char code.
    public int get8charValue(String code) {
        return (int) (decode8char(code) & 0x000000FF);
    }

    // Return the compare value from an 8 char code.
    public int get8charCompare(String code) {
        return (int) (decode8char(code) >> 23) & 0xFF;
    }
}