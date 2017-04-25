public interface InputHandler{
	
	// Joypad keys:
	public static final int KEY_A =      0;
	public static final int KEY_B =      1;
	public static final int KEY_START =  2;
	public static final int KEY_SELECT = 3;
	public static final int KEY_UP =     4;
	public static final int KEY_DOWN =   5;
	public static final int KEY_LEFT =   6;
	public static final int KEY_RIGHT =  7;
	
	// Key count:
	public static final int NUM_KEYS  =  8;
	
	public short getKeyState(int padKey);
	public void mapKey(int padKey, int deviceKey);
	public void reset();
	public void update();
	
}