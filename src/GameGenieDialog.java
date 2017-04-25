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
import javax.swing.*;
import javax.swing.table.*;

public class GameGenieDialog extends JDialog {

    NES nes;
    GameGenie gg;
    JTable tblCodes;
    JScrollPane codeScroll;
    MyTableModel model;

    public GameGenieDialog(GameGenie gg, NES nes) {

        this.setTitle("vNES Game Genie");
        this.setSize(500, 250);
        this.gg = gg;
        this.nes = nes;
        initComponents();

    }

    private void initComponents() {

        Container cp = getContentPane();

        model = new MyTableModel();
        tblCodes = new JTable(model);
        codeScroll = new JScrollPane(tblCodes);
        cp.setLayout(null);
        cp.add(codeScroll);
        codeScroll.setBounds(8, 8, 480, 175);

    }

    private class MyTableModel extends AbstractTableModel {

        public int getColumnCount() {
            return 4;
        }

        public int getRowCount() {
            return gg.getCodeCount() + 1;
        }

        public Object getValueAt(int row, int column) {

            if (row < gg.getCodeCount()) {
                switch (column) {
                    case 0:
                        return gg.getCodeString(row).toUpperCase();
                    case 1:
                        return "$" + Misc.hex16(gg.getCodeAddress(row));
                    case 2:
                        return "$" + Misc.hex8(gg.getCodeValue(row));
                    case 3:
                        return "$" + Misc.hex8(gg.getCodeCompare(row));
                    default:
                        return "";
                }
            } else {
                return "";
            }

        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Code";
                case 1:
                    return "Address";
                case 2:
                    return "Value";
                case 3:
                    return "Cmp";
                default:
                    return "-";
            }
        }

        public boolean isCellEditable(int row, int col) {
            return (col == 0);
        }

        public void setValueAt(Object val, int row, int col) {

            if ((val != null && val instanceof String) && !((String) val).equals("")) {
                String s = ((String) val).trim();
                if (s.length() == 6 || s.length() == 8) {

                    if (row < gg.getCodeCount()) {
                        gg.editCode(row, s);
                    } else {
                        gg.addCode(s);
                    }

                }
            } else if (row < gg.getCodeCount()) {
                gg.removeCode(row);
            }

            if (gg.getCodeCount() > 0) {
                // Enable game genie:
                //System.out.println("Game Genie enabled.");
                nes.setGameGenieState(true);
            } else {
                // Disable game genie:
                //System.out.println("Game Genie disabled.");
                nes.setGameGenieState(false);
            }

            if (val != null) {
                tblCodes.updateUI();
            }

        }
    }

    public void destroy() {

        nes = null;
        gg = null;

    }
}