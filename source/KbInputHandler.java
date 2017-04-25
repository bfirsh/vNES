
import java.awt.event.*;

public class KbInputHandler implements KeyListener, InputHandler {

    boolean[] allKeysState;
    int[] keyMapping;
    int id;
    NES nes;

    public KbInputHandler(NES nes, int id) {
        this.nes = nes;
        this.id = id;
        allKeysState = new boolean[255];
        keyMapping = new int[InputHandler.NUM_KEYS];
    }

    public short getKeyState(int padKey) {
        return (short) (allKeysState[keyMapping[padKey]] ? 0x41 : 0x40);
    }

    public void mapKey(int padKey, int kbKeycode) {
        keyMapping[padKey] = kbKeycode;
    }

    public void keyPressed(KeyEvent ke) {

        int kc = ke.getKeyCode();
        if (kc >= allKeysState.length) {
            return;
        }

        allKeysState[kc] = true;

        // Can't hold both left & right or up & down at same time:
        if (kc == keyMapping[InputHandler.KEY_LEFT]) {
            allKeysState[keyMapping[InputHandler.KEY_RIGHT]] = false;
        } else if (kc == keyMapping[InputHandler.KEY_RIGHT]) {
            allKeysState[keyMapping[InputHandler.KEY_LEFT]] = false;
        } else if (kc == keyMapping[InputHandler.KEY_UP]) {
            allKeysState[keyMapping[InputHandler.KEY_DOWN]] = false;
        } else if (kc == keyMapping[InputHandler.KEY_DOWN]) {
            allKeysState[keyMapping[InputHandler.KEY_UP]] = false;
        }
    }

    public void keyReleased(KeyEvent ke) {

        int kc = ke.getKeyCode();
        if (kc >= allKeysState.length) {
            return;
        }

        allKeysState[kc] = false;

        if (id == 0) {
            switch (kc) {
                case KeyEvent.VK_F5: {
                    // Reset game:
                    if (nes.isRunning()) {
                        nes.stopEmulation();
                        nes.reset();
                        nes.reloadRom();
                        nes.startEmulation();
                    }
                    break;
                }
                case KeyEvent.VK_F10: {
                    // Just using this to display the battery RAM contents to user.
                    if (nes.rom != null) {
                        nes.rom.closeRom();
                    }
                    break;
                }
            }
        }

    }

    public void keyTyped(KeyEvent ke) {
        // Ignore.
    }

    public void reset() {
        allKeysState = new boolean[255];
    }

    public void update() {
        // doesn't do anything.
    }

    public void destroy() {
        nes = null;
    }
}