public class Mapper007 extends MapperDefault{
	
	int currentOffset;
	int currentMirroring;
	
	short[] prgrom;
	
	public void init(NES nes){
		
		super.init(nes);
		currentOffset    =  0;
		currentMirroring = -1;
		
		// Get ref to ROM:
		ROM rom = nes.getRom();
		
		// Read out all PRG rom:
		int bc = rom.getRomBankCount();
		prgrom = new short[bc*16384];
		for(int i=0;i<bc;i++){
			System.arraycopy(rom.getRomBank(i),0,prgrom,i*16384,16384);
		}
		
	}
	
	public short load(int address){
		
		if(address < 0x8000){
			
			// Register read
			return super.load(address);
			
		}else{
			
			// Use the offset to determine where to read from:
			// This is kind of a hack, but it seems to work.
			
			if((address+currentOffset)>=262144){
			return prgrom[(address+currentOffset)-262144];
			}
		
			else{
			return prgrom[address+currentOffset];
			
			}
		
		}
	}
	
	public void write(int address, short value){
		
		if(address < 0x8000){
			
			// Let the base mapper take care of it.
			super.write(address,value);
			
		}else{
			
			// Set PRG offset:
			currentOffset = ((value&0xF)-1) << 15;
			
			// Set mirroring:
			if(currentMirroring != (value&0x10)){
				
				currentMirroring = value&0x10;
				if(currentMirroring == 0){
					nes.getPpu().setMirroring(ROM.SINGLESCREEN_MIRRORING);
				}else{
					nes.getPpu().setMirroring(ROM.SINGLESCREEN_MIRRORING2);
				}
				
			}
			
		}
		
	}
	
	public void mapperInternalStateLoad(ByteBuffer buf){
		
		super.mapperInternalStateLoad(buf);
		
		// Check version:
		if(buf.readByte()==1){
			
			currentMirroring = buf.readByte();
			currentOffset    = buf.readInt();
			
		}
		
	}
	
	public void mapperInternalStateSave(ByteBuffer buf){
		
		super.mapperInternalStateSave(buf);
		
		// Version:
		buf.putByte((short)1);
		
		// State:
		buf.putByte((short)currentMirroring);
		buf.putInt (currentOffset   );
		
	}
	
	public void reset(){
		
		super.reset();
		currentOffset    =  0;
		currentMirroring = -1;
		
	}
	
}