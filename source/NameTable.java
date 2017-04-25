public class NameTable{
	
	String name;
	
	short[] tile;
	short[] attrib;
	
	int width;
	int height;
	
	public NameTable(int width, int height, String name){
		
		this.name = name;
		
		this.width = width;
		this.height = height;
		
		tile = new short[width*height];
		attrib = new short[width*height];
		
	}
	
	public short getTileIndex(int x, int y){
		
		return tile[y*width+x];
		
	}
	
	public short getAttrib(int x, int y){
		
		return attrib[y*width+x];
		
	}
	
	public void writeTileIndex(int index, int value){
		
		tile[index] = (short)value;
		
	}
	
	public void writeAttrib(int index, int value){
		
		int basex,basey;
		int add;
		int tx,ty;
		int attindex;
		basex = index%8;
		basey = index/8;
		basex *= 4;
		basey *= 4;
		
		for(int sqy=0;sqy<2;sqy++){
			for(int sqx=0;sqx<2;sqx++){
				add = (value>>(2*(sqy*2+sqx)))&3;
				for(int y=0;y<2;y++){
					for(int x=0;x<2;x++){
						tx = basex+sqx*2+x;
						ty = basey+sqy*2+y;
						attindex = ty*width+tx;
						attrib[ty*width+tx] = (short)((add<<2)&12);
						////System.out.println("x="+tx+" y="+ty+" value="+attrib[ty*width+tx]+" index="+attindex);
					}
				}
			}
		}
		
	}
	
	public void stateSave(ByteBuffer buf){
		
		for(int i=0;i<width*height;i++){
			if(tile[i]>255)//System.out.println(">255!!");
			buf.putByte((byte)tile[i]);
		}
		for(int i=0;i<width*height;i++){
			buf.putByte((byte)attrib[i]);
		}
		
	}
	
	public void stateLoad(ByteBuffer buf){
		
		for(int i=0;i<width*height;i++){
			tile[i] = buf.readByte();
		}
		for(int i=0;i<width*height;i++){
			attrib[i] = buf.readByte();
		}
		
	}
	
	
}