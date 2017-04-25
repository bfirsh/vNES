import java.applet.*;
import java.awt.*;

public class vNES extends Applet implements Runnable{
	
	boolean scale;
	boolean scanlines;
	boolean sound;
	boolean fps;
	boolean stereo;
	boolean nicesound;
	boolean timeemulation;
	boolean showsoundbuffer;
	int samplerate;
	int romSize;
	int progress;
	
	AppletUI gui;
	NES nes;
	ScreenView panelScreen;
	String rom="";
	Font progressFont;
	Color bgColor = Color.black.darker().darker();
	boolean started = false;
	
	public void init(){
		
		readParams();
		System.gc();
		
		gui = new AppletUI(this);
		gui.init(false);
		
		Globals.appletMode = true;
		Globals.memoryFlushValue = 0x00; // make SMB1 hacked version work.
		
		nes = gui.getNES();
		nes.enableSound(sound);
		nes.reset();
		
	}
	
	public void addScreenView(){
		
		panelScreen = (ScreenView)gui.getScreenView();
		panelScreen.setFPSEnabled(fps);
		
		this.setLayout(null);
		
		if(scale){
			
			if(scanlines){
				panelScreen.setScaleMode(BufferView.SCALE_SCANLINE);
			}else{
				panelScreen.setScaleMode(BufferView.SCALE_NORMAL);
			}
			
			this.setSize(512,480);
			this.setBounds(0,0,512,480);
			panelScreen.setBounds(0,0,512,480);
			
		}else{
			
			panelScreen.setBounds(0,0,256,240);
			
		}
		
		this.setIgnoreRepaint(true);
		this.add(panelScreen);
		
	}
	
	public void start(){
		
		Thread t = new Thread(this);
		t.start();
		
	}
	
	public void run(){
		
		// Set font to be used for progress display of loading:
		progressFont = new Font("Tahoma",Font.TRUETYPE_FONT | Font.BOLD,12);
		
		// Can start painting:
		started = true;
		
		// Load ROM file:
		System.out.println("vNES 2.11 \u00A9 2006-2009 Jamie Sanders");
		System.out.println("For games and updates, see www.virtualnes.com");
		System.out.println("Use of this program subject to GNU GPL, Version 3.");

		nes.loadRom(rom);
		
		if(nes.rom.isValid()){
			
			// Add the screen buffer:
			addScreenView();
			
			// Set some properties:
			Globals.timeEmulation = timeemulation;
			nes.ppu.showSoundBuffer = showsoundbuffer;
			
			// Start emulation:
			//System.out.println("vNES is now starting the processor.");
			nes.getCpu().beginExecution();
			
		}else{
			
			// ROM file was invalid.
			System.out.println("vNES was unable to find ("+rom+").");
			
		}
		
	}
	
	public void stop(){
		nes.stopEmulation();
		//System.out.println("vNES has stopped the processor.");
		nes.getPapu().stop();
		this.destroy();
		
	}
	
	public void destroy(){
		
		if(nes!=null && nes.getCpu().isRunning()){
			stop();
		}
		//System.out.println("* Destroying applet.. *");
		
		if(nes!=null)nes.destroy();
		if(gui!=null)gui.destroy();
		
		gui = null;
		nes = null;
		panelScreen = null;
		rom = null;
		
		System.runFinalization();
		System.gc();
		
	}
	
	public void showLoadProgress(int percentComplete){
		
		progress = percentComplete;
		paint(getGraphics());
		
	}
	
	// Show the progress graphically.
	public void paint(Graphics g){

		String pad;
		String disp;
		int scrw,scrh;
		int txtw,txth;
		
		if(!started)return;
		
		// Get screen size:
		if(scale){
			scrw = 512;
			scrh = 480;
		}else{
			scrw = 256;
			scrh = 240;
		}
		
		// Fill background:
		g.setColor(bgColor);
		g.fillRect(0,0,scrw,scrh);
		
		// Prepare text:
		if(progress<10){
			pad = "  ";
		}else if(progress<100){
			pad = " ";
		}else{
			pad = "";
		}
		disp = "vNES is Loading Game... "+pad+progress+"%";		
		
		// Measure text:
		g.setFont(progressFont);
		txtw = g.getFontMetrics(progressFont).stringWidth(disp);
		txth = g.getFontMetrics(progressFont).getHeight();
		
		// Display text:
		g.setFont(progressFont);
		g.setColor(Color.white);
		g.drawString(disp,scrw/2-txtw/2,scrh/2-txth/2);
		g.drawString(disp,scrw/2-txtw/2,scrh/2-txth/2);
		g.drawString("vNES \u00A9 2006-2009 Jamie Sanders", 12, 448);
		g.drawString("For games and updates, visit www.virtualnes.com", 12, 464);	
	}
	
	public void update(Graphics g){
		// do nothing.
	}
	
	public void readParams(){
		
		String tmp;
		
		tmp = getParameter("rom");
		if(tmp==null || tmp.equals("")){
			rom = "vnes.nes";
		}else{
			rom = tmp;
		}
		
		tmp = getParameter("scale");
		if(tmp==null || tmp.equals("")){
			scale = false;
		}else{
			scale = tmp.equals("on");
		}
		
		tmp = getParameter("sound");
		if(tmp==null || tmp.equals("")){
			sound = true;
		}else{
			sound = tmp.equals("on");
		}
		
		tmp = getParameter("stereo");
		if(tmp==null || tmp.equals("")){
			stereo = true; // on by default
		}else{
			stereo = tmp.equals("on");
		}
		
		tmp = getParameter("scanlines");
		if(tmp==null || tmp.equals("")){
			scanlines = false;
		}else{
			scanlines = tmp.equals("on");
		}
		
		tmp = getParameter("fps");
		if(tmp==null || tmp.equals("")){
			fps = false;
		}else{
			fps = tmp.equals("on");
		}
		
		tmp = getParameter("nicesound");
		if(tmp==null || tmp.equals("")){
			nicesound = true;
		}else{
			nicesound = tmp.equals("on");
		}
		
		tmp = getParameter("timeemulation");
		if(tmp==null || tmp.equals("")){
			timeemulation = true;
		}else{
			timeemulation = tmp.equals("on");
		}
		
		tmp = getParameter("showsoundbuffer");
		if(tmp==null || tmp.equals("")){
			showsoundbuffer = false;
		}else{
			showsoundbuffer = tmp.equals("on");
		}
		
		tmp = getParameter("romsize");
		if(tmp==null || tmp.equals("")){
			romSize = -1;
		}else{
			try{
				romSize = Integer.parseInt(tmp);
			}catch(Exception e){
				romSize = -1;
			}
		}
		
	}
	
}