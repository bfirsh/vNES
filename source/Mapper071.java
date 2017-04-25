public class Mapper071 extends MapperDefault{
	
	int curBank;
	
	public void init(NES nes){
		
		super.init(nes);
		reset();
		
	}
	
	public void loadROM(ROM rom){
	
		//System.out.println("Loading ROM.");
	
		if(!rom.isValid()){
			//System.out.println("Camerica: Invalid ROM! Unable to load.");
			return;
		}
		
		// Get number of PRG ROM banks:
		int num_banks = rom.getRomBankCount();
		
		// Load PRG-ROM:
		loadRomBank(          0,0x8000);
		loadRomBank(num_banks-1,0xC000);
		
		// Load CHR-ROM:
		loadCHRROM();
		
		// Load Battery RAM (if present):
		loadBatteryRam();
		
		// Do Reset-Interrupt:
		nes.getCpu().requestIrq(CPU.IRQ_RESET);
		
	}
	
	public void write(int address, short value){
		
		if(address < 0x8000){
			
			// Handle normally:
			super.write(address,value);
			
		}else if(address < 0xC000){
			
			// Unknown function.
			
		}else{
			
			// Select 16K PRG ROM at 0x8000:
			if(value != curBank){
				
				curBank = value;
				loadRomBank(value,0x8000);
				
			}
			
		}
		
	}
	
	public void reset(){
		
		curBank = -1;
		
	}
	
}