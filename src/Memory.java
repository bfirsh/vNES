
import java.io.*;

public class Memory{
	
	public short[] mem;
	int memLength;
	NES nes;
	
	public Memory(NES nes, int byteCount){
		this.nes = nes;
		mem = new short[byteCount];
		memLength = byteCount;
	}
	
	public void stateLoad(ByteBuffer buf){
		
		if(mem==null)mem=new short[memLength];
		buf.readByteArray(mem);
		
	}
	
	public void stateSave(ByteBuffer buf){
		
		buf.putByteArray(mem);
		
	}
	
	public void reset(){
		for(int i=0;i<mem.length;i++)mem[i] = 0;
	}
	
	public int getMemSize(){
		return memLength;
	}
	
	public void write(int address, short value){
		mem[address] = value;
	}
	
	public short load(int address){
		return mem[address];
	}
	
	public void dump(String file){
		dump(file,0,mem.length);
	}
	
	public void dump(String file, int offset, int length){
		
		char[] ch = new char[length];
		for(int i=0;i<length;i++){
			ch[i] = (char)mem[offset+i];
		}
		
		try{
			
			File f = new File(file);
			FileWriter writer = new FileWriter(f);
			writer.write(ch);
			writer.close();
			//System.out.println("Memory dumped to file "+file+".");
			
		}catch(IOException ioe){
			//System.out.println("Memory dump to file: IO Error!");
		}
		
		
	}
	
	public void write(int address, short[] array, int length){
	
		if(address+length > mem.length)return;
		System.arraycopy(array,0,mem,address,length);
		
	}
	
	public void write(int address, short[] array, int arrayoffset, int length){
		
		if(address+length > mem.length)return;
		System.arraycopy(array,arrayoffset,mem,address,length);
		
	}
	
	public void destroy(){
	
		nes = null;
		mem = null;
		
	}
	
}