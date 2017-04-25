
public interface MemoryMapper{
	
	public void init(NES nes);
	public void loadROM(ROM rom);
	public void write(int address, short value);
	public short load(int address);
	public short joy1Read();
	public short joy2Read();
	public void reset();
	public void setGameGenieState(boolean value);
	public void clockIrqCounter();
	public void loadBatteryRam();
	public void destroy();
	public void stateLoad(ByteBuffer buf);
	public void stateSave(ByteBuffer buf);
	public void setMouseState(boolean pressed, int x, int y);
	public void latchAccess(int address);
	
}