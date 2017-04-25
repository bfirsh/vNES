import java.awt.event.*;

public class AppletUI implements UI{
	
	vNES applet;
	NES nes;
	KbInputHandler kbJoy1;
	KbInputHandler kbJoy2;
	ScreenView vScreen;
	HiResTimer timer;
	
	long t1,t2;
	int sleepTime;
	
	public AppletUI(vNES applet){
	
		timer = new HiResTimer();
		this.applet = applet;
		nes = new NES(this);
	
	}
	
	public void init(boolean showGui){
		
		vScreen = new ScreenView(nes,256,240);
		vScreen.setBgColor(applet.bgColor.getRGB());
		vScreen.init();
		vScreen.setNotifyImageReady(true);
		
		kbJoy1 = new KbInputHandler(nes,0);
		kbJoy2 = new KbInputHandler(nes,1);
		
		// Map keyboard input keys for joypad 1:
		kbJoy1.mapKey(InputHandler.KEY_A,KeyEvent.VK_X);
		kbJoy1.mapKey(InputHandler.KEY_B,KeyEvent.VK_Z);
		kbJoy1.mapKey(InputHandler.KEY_START,KeyEvent.VK_ENTER);
		kbJoy1.mapKey(InputHandler.KEY_SELECT,KeyEvent.VK_CONTROL);
		kbJoy1.mapKey(InputHandler.KEY_UP,KeyEvent.VK_UP);
		kbJoy1.mapKey(InputHandler.KEY_DOWN,KeyEvent.VK_DOWN);
		kbJoy1.mapKey(InputHandler.KEY_LEFT,KeyEvent.VK_LEFT);
		kbJoy1.mapKey(InputHandler.KEY_RIGHT,KeyEvent.VK_RIGHT);
		vScreen.addKeyListener(kbJoy1);

		// Map keyboard input keys for joypad 2:
		kbJoy2.mapKey(InputHandler.KEY_A,KeyEvent.VK_NUMPAD7);
		kbJoy2.mapKey(InputHandler.KEY_B,KeyEvent.VK_NUMPAD9);
		kbJoy2.mapKey(InputHandler.KEY_START,KeyEvent.VK_NUMPAD1);
		kbJoy2.mapKey(InputHandler.KEY_SELECT,KeyEvent.VK_NUMPAD3);
		kbJoy2.mapKey(InputHandler.KEY_UP,KeyEvent.VK_NUMPAD8);
		kbJoy2.mapKey(InputHandler.KEY_DOWN,KeyEvent.VK_NUMPAD2);
		kbJoy2.mapKey(InputHandler.KEY_LEFT,KeyEvent.VK_NUMPAD4);
		kbJoy2.mapKey(InputHandler.KEY_RIGHT,KeyEvent.VK_NUMPAD6);
		vScreen.addKeyListener(kbJoy2);
		
	}
	
	public void imageReady(boolean skipFrame){
		
		// Sound stuff:
		int tmp = nes.getPapu().bufferIndex;
		if(Globals.enableSound && Globals.timeEmulation && tmp>0){
			
			int min_avail = nes.getPapu().line.getBufferSize()-4*tmp;
			timer.sleepMicros(nes.papu.getMillisToAvailableAbove(min_avail));
			while(nes.getPapu().line.available() < min_avail){
				timer.yield();
			}
			nes.getPapu().writeBuffer();

		}
		
		// Sleep a bit if sound is disabled:
		if(Globals.timeEmulation && !Globals.enableSound){
			
			sleepTime = Globals.frameTime;
			if((t2=timer.currentMicros())-t1 < sleepTime){
				timer.sleepMicros(sleepTime-(t2-t1));
			}
			
		}

		// Update timer:
		t1 = t2;
		
	}
	
	public int getRomFileSize(){
		return applet.romSize;
	}
	
	public void showLoadProgress(int percentComplete){
		
		// Show ROM load progress:
		applet.showLoadProgress(percentComplete);
		
		// Sleep a bit:
		timer.sleepMicros(20*1000);
		
	}
	
	public void destroy(){
		
		if(vScreen!=null)vScreen.destroy();
		if(kbJoy1!=null)kbJoy1.destroy();
		if(kbJoy2!=null)kbJoy2.destroy();
		
		nes = null;
		applet = null;
		kbJoy1 = null;
		kbJoy2 = null;
		vScreen = null;
		timer = null;
		
	}
	
	public NES getNES(){
		return nes;
	}
	public InputHandler getJoy1(){
		return kbJoy1;
	}
	public InputHandler getJoy2(){
		return kbJoy2;
	}
	public BufferView getScreenView(){
		return vScreen;
	}
	public BufferView getPatternView(){
		return null;
	}
	public BufferView getSprPalView(){
		return null;
	}
	public BufferView getNameTableView(){
		return null;
	}
	public BufferView getImgPalView(){
		return null;
	}
	public HiResTimer getTimer(){
		return timer;
	}
	public String getWindowCaption(){
		return "";
	}
	public void setWindowCaption(String s){}
	public void setTitle(String s){}
	public java.awt.Point getLocation(){
		return new java.awt.Point(0,0);
	}
	public int getWidth(){
		return applet.getWidth();
	}
	public int getHeight(){
		return applet.getHeight();
	}
	public void println(String s){}
	public void showErrorMsg(String msg){System.out.println(msg);}
	
}