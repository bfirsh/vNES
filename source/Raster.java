public class Raster{
	
	public int[] data;
	public int width;
	public int height;
	
	public Raster(int[] data, int w, int h){
		this.data = data;
		width = w;
		height = h;
	}
	
	public Raster(int w, int h){
		data = new int[w*h];
		width = w;
		height = h;
	}
	
	public void drawTile(Raster srcRaster, int srcx, int srcy, int dstx, int dsty, int w, int h){
		
		int[] src = srcRaster.data;
		int src_index;
		int dst_index;
		int tmp;
		
		for(int y=0;y<h;y++){
			
			src_index = (srcy + y) *  srcRaster.width + srcx;
			dst_index = (dsty + y) * width + dstx;
			
			for(int x=0;x<w;x++){
				
				if((tmp = src[src_index]) != 0){
					data[dst_index] = tmp;
				}
				
				src_index++;
				dst_index++;
				
			}
		}
		
	}
	
}