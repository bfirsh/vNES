public class BlipBuffer{
	
	// These values must be set:
	public int win_size;
	public int smp_period;	
	public int sinc_periods;
	
	// Different samplings of bandlimited impulse:
	public int[][] imp;
	
	// Difference buffer:
	public int[] diff;
	
	// Last position changed in buffer:
	int lastChanged;
	
	// Previous end absolute value:
	int prevSum;
	
	// DC removal:
	int dc_prev;
	int dc_diff;
	int dc_acc;
	
	public void init(int bufferSize, int windowSize, int samplePeriod, int sincPeriods){
		
		win_size	 = windowSize;
		smp_period	 = samplePeriod;
		sinc_periods = sincPeriods;
		double[] buf = new double[smp_period*win_size];
		
		
		// Sample sinc:
		double si_p = sinc_periods;
		for(int i=0;i<buf.length;i++){
			buf[i] = sinc(-si_p*Math.PI + (si_p*2.0*((double)i)*Math.PI)/((double)buf.length));
		}
		
		// Fill into impulse buffer:
		imp = new int[smp_period][win_size];
		for(int off=0;off<smp_period;off++){
			double sum = 0;
			for(int i=0;i<win_size;i++){
				sum += 32768.0*buf[i*smp_period+off];
				imp[smp_period-1-off][i] = (int)sum;
			}
		}
		
		// Create difference buffer:
		diff = new int[bufferSize];
		lastChanged = 0;
		prevSum = 0;
		dc_prev = 0;
		dc_diff = 0;
		dc_acc  = 0;
		
	}
	
	public void impulse(int smpPos, int smpOffset, int magnitude){
		
		// Add into difference buffer:
		//if(smpPos+win_size < diff.length){
			for(int i=lastChanged;i<smpPos+win_size;i++){
				diff[i] = prevSum;
			}
			for(int i=0;i<win_size;i++){
				diff[smpPos+i] += (imp[smpOffset][i]*magnitude)>>8;
			}
			lastChanged = smpPos+win_size;
			prevSum = diff[smpPos+win_size-1];
		//}
		
	}
	
	public int integrate(){
		
		int sum=prevSum;
		for(int i=0;i<diff.length;i++){
			
			sum += diff[i];
			
			// Remove DC:
			dc_diff  = sum - dc_prev;
			dc_prev += dc_diff;
			dc_acc  += dc_diff - (dc_acc >> 10);
			diff[i]  = dc_acc;
			
		}
		prevSum = sum;
		return lastChanged;
		
	}
	
	public void clear(){
		
		for(int i=0;i<diff.length;i++)diff[i]=0;
		lastChanged = 0;
		
	}
	
	public static double sinc(double x){
		if(x==0.0)return 1.0;
		return Math.sin(x)/x;
	}
	
}