public class HiResTimer{
	
	public long currentMicros(){
		return System.nanoTime()/1000;
	}
	
	public long currentTick(){
		return System.nanoTime();
	}
	
	public void sleepMicros(long time){
		
		try{
			
			Thread.yield();
			long nanos = time - (time/1000)*1000;
			if(nanos > 999999)nanos = 999999;
			Thread.sleep(time/1000,(int)nanos);
			
		}catch(Exception e){
			
			//System.out.println("Sleep interrupted..");
			e.printStackTrace();
			
		}
		
	}
	
	public void sleepMillisIdle(int millis){
		
		millis /= 10;
		millis *= 10;
		
		try{
			Thread.sleep(millis);
		}catch(InterruptedException ie){}
		
	}
	
	public void yield(){
		Thread.yield();
	}
	
}