public interface PapuChannel{
	
	public void writeReg(int address, int value);
	public void setEnabled(boolean value);
	public boolean isEnabled();
	public void reset();
	public int getLengthStatus();
	
}