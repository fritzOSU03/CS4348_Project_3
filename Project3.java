import java.util.Scanner;


/**
 * Project3.java
 * 
 */
public class Project3 {
	protected static Scanner scanner = new Scanner(System.in);
	protected static boolean debugMode = false;
	
	public static void main(String[] args) {
		
		if(args.length > 0)
			// Start Simulation if Allocation Type Provided
			switch(args[0].toLowerCase()) {
			case "chained":
				try {new DiskInterface(AllocationType.CHAINED);}
				catch(Exception e) {e.printStackTrace();}
				break;
			case "indexed":
				try {new DiskInterface(AllocationType.INDEXED);}
				catch(Exception e) {e.printStackTrace();}
				break;
			case "contiguous":
				try {new DiskInterface(AllocationType.CONTIGUOUS);}
				catch(Exception e) {e.printStackTrace();}
				break;
			default:
				System.out.println("Invalid allocation type.");
				System.out.println("Usage: java Project3 [chained | indexed | contiguous]");
			}
		else {
			// Prompt for Allocation Type
			String[] header = {"Select the disk allocation method."};
			String[] options = {"Chained", "Indexed", "Contiguous"};
			try {new DiskInterface(AllocationType.values()[DiskInterfaceUtils.optionChooser(header, options, 25) - 1]);}
			catch(Exception e) {e.printStackTrace();}
		}
	}
}
