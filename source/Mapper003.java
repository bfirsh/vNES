public class Mapper003 extends MapperDefault{
	
	public void init(NES nes){
		
		super.init(nes);
		
	}
	
	public void write(int address, short value){
		
		if(address < 0x8000){
			
			// Let the base mapper take care of it.
			super.write(address,value);
			
		}else{
			
			// This is a VROM bank select command.
			// Swap in the given VROM bank at 0x0000:
			int bank = (value%(nes.getRom().getVromBankCount()/2))*2;
			loadVromBank(bank,0x0000);
			loadVromBank(bank+1,0x1000);
			load8kVromBank(value*2,0x0000);
			
		}
		
	}
	
	
}