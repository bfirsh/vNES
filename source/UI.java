import java.awt.*;

public interface UI{
	
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