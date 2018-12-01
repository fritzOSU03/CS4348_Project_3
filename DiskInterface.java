import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.ArrayList;


/**
 * DiskInterface.java
 * 
 */
public class DiskInterface {
	protected final static int NUM_BLOCKS = 256;
	protected final static int BLOCK_SIZE = 512;
	protected final static int CHAINED_BLOCK_SIZE = BLOCK_SIZE - 1;
	protected static int recordSize;
	protected static int maxRecords;
	protected static AllocationType type;
	protected static Disk disk;
	protected static ArrayList<FileRecord> fileAllocationTable = new ArrayList<FileRecord>();
	
	
	/**
	 * Constructs the {@code DistInterface} object with the
	 * {@code AllocationType} set to the given value.
	 * 
	 * @param   type
	 * 	        A disk {@code AllocationType}.
	 * @throws  Exception 
	 *          If the {@code BLOCK_SIZE} is less than the
	 *          {@code NUM_BLOCKS}.
	 * @throws  IndexOutOfBoundsException
	 * 	        If the {@code type} is not a valid {@code AllocationType}
	 * 	        value.
	 * @see     AllocationType
	 */
	@SuppressWarnings("unused")
	public DiskInterface(AllocationType type) throws Exception {
		if(BLOCK_SIZE < NUM_BLOCKS) throw new Exception("Error: Block size must not be smaller than the number of blocks.");
		
		DiskInterface.disk = new Disk(NUM_BLOCKS, BLOCK_SIZE);
		DiskInterface.type = type;
		
		switch(DiskInterface.type) {
		case CHAINED:
			recordSize = 10;
			maxRecords = BLOCK_SIZE/recordSize;
			break;
		case INDEXED:
			recordSize = 9;
			maxRecords = BLOCK_SIZE/recordSize;
			break;
		case CONTIGUOUS:
			recordSize = 10;
			maxRecords = BLOCK_SIZE/recordSize;
			break;
		default:
			throw new IndexOutOfBoundsException("Invalid disk AllocationType.");
		}
		
		runSimulation();
	}
	
	/**
	 * This is the main control loop for the application.
	 */
	private void runSimulation() {
		String[] header = {"Select a disk function."};
		String[] options = {
				"Display a file",
				"Display the file table",
				"Display the free space bitmap",
				"Display a disk block",
				"Copy a file from the simulation to a file on the real system",
				"Copy a file from the real system to a file in the simulation",
				"Delete a file",
				"Exit"};
		
		while(true) {
			switch(DiskInterfaceUtils.optionChooser(header, options, 4)) {
			case 1:
				displayFile();
				break;
			case 2:
				displayFileTable();
				break;
			case 3:
				displayFreeSpace(false);
				break;
			case 4:
				displayDiskBlock();
				break;
			case 5:
				copyFromDisk();
				break;
			case 6:
				copyToDisk();
				break;
			case 7:
				deleteFile();
				break;
			case 8:
				shutDown();
				break;
			}
		}
	}
	
	/**
	 * Prompts for a file by name to display and shows the file on screen
	 * in character format to the user after verifying that it exists in
	 * the disk.
	 */
	private void displayFile() {
		if(Project3.debugMode) System.out.println(getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "() called.");
		
		// Update the FAT. Check for files.
		if(DiskInterfaceUtils.getFAT() > 0) {
			String fileName = "";
			
			// Prompt for file name.
			while(!DiskInterfaceUtils.isValidFile(fileName)) {
				try {
					System.out.print("\nName of file: ");
					
					if(Project3.scanner.hasNextLine())
						fileName = Project3.scanner.nextLine().trim();
					
					if(fileName.length() > 4 && fileName.length() <= 8) break;
					else System.out.println("Invalid entry. Please enter a valid file name. E.g. \"text.txt\".");
				}
				catch(Exception e) {System.out.println("Invalid entry. Please try again.");}
			}
			
			// Get file record info from FAT.
			FileRecord fileRecord = DiskInterfaceUtils.getFileRecord(fileName);
			int block = 0, length = 0;
			
			// Get file bytes based on allocation type.
			switch(type) {
			case CHAINED:
				// FAT Chained: File Name, Start Block, Length
				block = fileRecord.getStartBlock();
				length = fileRecord.getLength();
				
				System.out.println("\nContents of " + fileName + ":");
				// Chained: Random free block. Last bit points to next free block.
				while(true) {
					if(length > 1) {
						for(int i = 0; i < CHAINED_BLOCK_SIZE; i++)
							System.out.print((char)(disk.read(block, i) & 0xFF));
						length--;
						block = (disk.read(block, CHAINED_BLOCK_SIZE) & 0xFF);
					}
					else {
						for(int i = 0; i < BLOCK_SIZE; i++)
							System.out.print((char)(disk.read(block, i) & 0xFF));
						System.out.println();
						break;
					}
				}
				break;
			case INDEXED:
				// FAT Indexed: File Name, Index Block
				block = fileRecord.getIndexBlock();
				
				// Get indexes from index block.
				ArrayList<Integer> indexes = new ArrayList<Integer>();
				for(int i = 0; i < 10; i++) {
					if(disk.read(block, i) == 0) break;
					indexes.add(Byte.toUnsignedInt(disk.read(block, i)));
					length++;
				}
				
				System.out.println("\nContents of " + fileName + ":");
				// Indexed: Random Free block to store an ordered list of other random free blocks holding the file.
				while(true) {
					block = indexes.remove(0);
					
					if(length > 1) {
						for(int i = 0; i < BLOCK_SIZE; i++)
							System.out.print((char)(disk.read(block, i) & 0xFF));
						length--;
					}
					else {
						for(int i = 0; i < BLOCK_SIZE ; i++)
							System.out.print((char)(disk.read(block, i) & 0xFF));
						System.out.println();
						break;
					}
				}
				break;
			case CONTIGUOUS:
				// FAT Contiguous: File Name, Start Block, Length
				block = fileRecord.getStartBlock();
				length = fileRecord.getLength();
				
				System.out.println("\nContents of " + fileName + ":");
				// Contiguous: First available set of blocks large enough.
				while(true) {
					if(length > 1) {
						for(int i = 0; i < BLOCK_SIZE; i++)
							System.out.print((char)(disk.read(block, i) & 0xFF));
						block++;
						length--;
					}
					else {
						for(int i = 0; i < BLOCK_SIZE ; i++)
							System.out.print((char)(disk.read(block, i) & 0xFF));
						System.out.println();
						break;
					}
				}
				break;
			}
		}
		// Print message if no files on disk.
		else
			System.out.println("\nThere are no files on the disk to show.");
		
		System.out.print("Press Enter to continue");
		Project3.scanner.nextLine();
	}
	
	/**
	 * Displays the file allocation table. Formatting is applied based on
	 * allocation type. 
	 */
	private void displayFileTable() {
		if(Project3.debugMode) System.out.println(getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "() called.");
		
		// Get the number of records in the FAT.
		int numRecords = DiskInterfaceUtils.getFAT();
		
		System.out.println();
		if(numRecords > 0) {
			// Display the file info based on allocation type.
			switch(type) {
			case CHAINED:
				// Chained: File Name, Start Block, Length
				System.out.println("----------------------------------------");
				System.out.printf("%-15s%15s%10s\n", "File Name", "Start Block", "Length");
				System.out.println("----------------------------------------");
				
				for(int i = 0; i < numRecords; i++)
					if(fileAllocationTable.get(i).getFileName() != null)
						System.out.printf("%-15s%15d%10d\n", fileAllocationTable.get(i).getFileName(), fileAllocationTable.get(i).getStartBlock(), fileAllocationTable.get(i).getLength());
				
				break;
			case INDEXED:
				// Indexed: File Name, Index Block
				System.out.println("------------------------------");
				System.out.printf("%-15s%15s\n", "File Name", "Index Block");
				System.out.println("------------------------------");
				
				for(int i = 0; i < numRecords; i++)
					if(fileAllocationTable.get(i).getFileName() != null)
						System.out.printf("%-15s%15d\n", fileAllocationTable.get(i).getFileName(), fileAllocationTable.get(i).getIndexBlock());
				
				break;
			case CONTIGUOUS:
				// Contiguous: File Name, Start Block, Length
				System.out.println("----------------------------------------");
				System.out.printf("%-15s%15s%10s\n", "File Name", "Start Block", "Length");
				System.out.println("----------------------------------------");
				
				for(int i = 0; i < numRecords; i++)
					if(fileAllocationTable.get(i).getFileName() != null)
						System.out.printf("%-15s%15d%10d\n", fileAllocationTable.get(i).getFileName(), fileAllocationTable.get(i).getStartBlock(), fileAllocationTable.get(i).getLength());
				
				break;
			}
		}
		else System.out.print("There are no file records in the file table.");
		
		System.out.print("\nPress Enter to continue");
		Project3.scanner.nextLine();
	}
	
	/**
	 * Prints a bitmap to the console containing a value for each block on
	 * the disk. A 0 is printed if the block is empty and a 1 is printed
	 * if the block is not empty.
	 * 
	 * @param   enhanced
	 *          A boolean value for a more formatted version of output.
	 */
	private void displayFreeSpace(boolean enhanced) {
		if(Project3.debugMode) System.out.println(getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "() called.");
		
		if(enhanced) {
			
			// Determine the necessary block column character width.
			int zeros = 0;
			int blocks = NUM_BLOCKS;
			while(blocks > 0) {
				zeros++;
				blocks /= 10;
			}
			
			// Create the print formatters.
			String hdrPfx1 = "|";
			String hdrPfx2 = "|";
			String fmtPfx = "|%" + zeros + "d";
			String ftrPfx = "|";
			
			for(int i = 0; i < zeros; i++) {
				hdrPfx1 += "-";
				hdrPfx2 += (i == (zeros/2 + 1) ? "B" : " ");
				ftrPfx += "-";
			}
			
			// Update the File System Bitmap.
			DiskInterfaceUtils.updateBitmap();
			
			// Print the header.
			System.out.println("\n" + hdrPfx1 + "|-----------------------------------------------------------------|");
			System.out.println(       hdrPfx2 + "|                    Current Free Space Bitmap                    |");
			System.out.println(       hdrPfx1 + "|-----------------------------------------------------------------|");
			System.out.print(String.format(fmtPfx, 0) + "|");
			
			// Iterate the blocks and print.
			for(int i = 0; i < NUM_BLOCKS; i++) {
				System.out.print(i != 1 && disk.read(1, i) == 0 ? " " + 0 : " " + 1);
				System.out.print(i + 1 < NUM_BLOCKS && (i + 1) % 32 == 0 ? " |\n" + String.format(fmtPfx, i) + "|" : "");
			}
			
			// Print the footer.
			System.out.println(" |\n" + hdrPfx1 + ftrPfx + "-------------------------------------------------------------" + (zeros % 2 == 0 ? "|" : "-|"));
		}
		else {
			// Update the File System Bitmap.
			DiskInterfaceUtils.updateBitmap();
			
			// Print the header.
			System.out.println("\n--------------------------------");
			System.out.println(  "|  Current Free Space Bitmap   |");
			System.out.println(  "--------------------------------");
			
			// Iterate the blocks and print.
			for(int i = 0; i < NUM_BLOCKS; i++) {
				System.out.print(i != 1 && disk.read(1, i) == 0 ? 0 : 1);
				System.out.print(i + 1 < NUM_BLOCKS && (i + 1) % 32 == 0 ? "\n" : "");
			}
			
			// Print the footer.
			System.out.println("\n--------------------------------");
		}
		
		System.out.print("\nPress Enter to continue");
		Project3.scanner.nextLine();
	}
	
	/**
	 * Shows the content of a block as unsigned integer values (ASCII
	 * values).
	 */
	private void displayDiskBlock() {
		if(Project3.debugMode) System.out.println(getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "() called.");
		int choice = -1;
		int tries = 3;
		
		// Prompt for block number.
		while(tries > 0) {
			try {
				System.out.print("\nEnter a block number: ");
				
				// Wait for input and store.
				if(Project3.scanner.hasNextLine())
					choice = Integer.parseInt(Project3.scanner.nextLine().trim());
				
				// Check for valid block choice.
				if(choice > 1 && choice < NUM_BLOCKS) break;
				else System.out.println("Invalid entry. Please enter a number from 2 to " + (NUM_BLOCKS - 1) + ".");
			}
			catch(Exception e) {System.out.println("Invalid entry. Please try again.");}
			
			tries--;
		}
		
		if(tries == 0) return;
		
		// Show complete block content.
		System.out.println("\nThe byte content of block " + choice + " is:");
		for(int i = 0; i < BLOCK_SIZE; i++)
			System.out.printf("%3d%s", Byte.toUnsignedInt(disk.read(choice, i)), ((i > 0 && (i + 1) % 32 == 0) ? "\n" : " "));
		
		System.out.print("Press Enter to continue");
		Project3.scanner.nextLine();
	}
	
	/**
	 * Copies a file from the disk to the local machine.
	 */
	private void copyFromDisk() {
		if(Project3.debugMode) System.out.println(getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "() called.");
		
		// Update the FAT. Check for files.
		if(DiskInterfaceUtils.getFAT() > 0) {
			File file = null;
			String fileName = "", newFileName = "";
			
			// Prompt for Disk source file name or Exit.
			while(true) {
				System.out.print("\nCopy from: ");
				if(Project3.scanner.hasNextLine())
					fileName = Project3.scanner.nextLine().trim();
				
				if(fileName.equalsIgnoreCase("Exit")) return;
				if(!fileName.equalsIgnoreCase("") && DiskInterfaceUtils.isValidFile(fileName)) break;
				
				System.out.println("Invalid file name. Please try again or type \'Exit\' to cancel.");
			}
			
			String path;
			
			// Prompt for local destination file name or Exit.
			while(true) {
				System.out.print("Copy to: ");
				if(Project3.scanner.hasNextLine())
					newFileName = Project3.scanner.nextLine().trim();
				
				if(newFileName.equalsIgnoreCase("Exit")) return;
				
				path = Project3.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "/" + newFileName;
				try {path = URLDecoder.decode(path, "utf-8");}
				catch(UnsupportedEncodingException e) {e.printStackTrace();}
				path = new File(path).getPath();
				if(Project3.debugMode) System.out.println(path);
				file = new File(path);
				
				if(newFileName.equalsIgnoreCase("")) newFileName = new String(fileName);
				
				// Do a file type check.
				if(newFileName.length() > 4 && newFileName.substring(newFileName.lastIndexOf("."), newFileName.length()).trim().equalsIgnoreCase(fileName.substring(fileName.lastIndexOf("."), fileName.length()).trim())) break;
				else System.out.println("Invalid file name or type. File types must match. Please try again or type \'Exit\' to cancel.");
				
				System.out.println("Invalid file name. Please try again or type \'Exit\' to cancel.");
			}
			
			// If the file exists, confirm overwrite.
			if(file.exists()) {
				System.out.print("\nA copy of " + newFileName + " already exists on your local disk. Overwrite? [y/n]: ");
				String temp = Project3.scanner.nextLine();
				char answer = (temp.length() > 0 ? temp.charAt(0) : 0);
				if(answer != 'y' && answer != 'Y') {
					System.out.println("\nFile copy cancelled.");
					System.out.print("Press Enter to continue");
					Project3.scanner.nextLine();
					return;
				}
			}
			
			// Create the file out stream and write.
			FileOutputStream stream = null;
			try {
				stream = new FileOutputStream(path);
				stream.write(DiskInterfaceUtils.getFileBytesFromDisk(fileName));
			}
			catch(FileNotFoundException e) {System.out.println("Somethign went wrong when setting up the stream. Please try again.");e.printStackTrace();}
			catch(IOException e) {System.out.println("Somethign went wrong when writing the file. Please try again.");e.printStackTrace();}
			finally {
				try {stream.close();}
				catch(IOException e) {System.out.println("Somethign went wrong when closing the stream. Please try again.");e.printStackTrace();}
			}
			
			System.out.println("File successfully written to the local disk.");
		}
		// Print message if no files on disk.
		else
			System.out.println("\nThere are no files on the disk to copy.");
		
		System.out.print("Press Enter to continue");
		Project3.scanner.nextLine();
	}
	
	/**
	 * Copies a file from the local machine to the disk.
	 */
	private void copyToDisk() {
		if(Project3.debugMode) System.out.println(getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "() called.");
		
		File file = null;
		String fileName = "", newFileName = "";
		
		// Prompt for local source file name or Exit.
		while(true) {
			System.out.print("\nCopy from: ");
			if(Project3.scanner.hasNextLine())
				fileName = Project3.scanner.nextLine().trim();
			
			if(fileName.equalsIgnoreCase("Exit")) return;
			
			String path = Project3.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "/" + fileName;
			try {path = URLDecoder.decode(path, "utf-8");}
			catch(UnsupportedEncodingException e) {e.printStackTrace();}
			path = new File(path).getPath();
			if(Project3.debugMode) System.out.println(path);
			file = new File(path);
			
			if(file.exists()) break;
			
			System.out.println("Invalid file name. Please try again or type \'Exit\' to cancel.");
		}
		
		// Prompt for Disk destination file name or Exit.
		while(true) {
			System.out.print("Copy to: ");
			if(Project3.scanner.hasNextLine())
				newFileName = Project3.scanner.nextLine().trim();
			
			if(newFileName.equalsIgnoreCase("Exit")) return;
			
			if(newFileName.equalsIgnoreCase("")) {
				newFileName = new String(fileName);
				break;
			}
			else if(!newFileName.equalsIgnoreCase("") && newFileName.length() > 4 && newFileName.length() <= 8) break;
			else System.out.println("Invalid file name. Please try again or type \'Exit\' to cancel.");
		}
		
		// If the file exists, confirm overwrite and remove existing from disk.
		if(DiskInterfaceUtils.isValidFile(newFileName)) {
			System.out.print("A copy of " + fileName + " already exists on the disk. Overwrite? [y/n]: ");
			String temp = Project3.scanner.nextLine();
			char answer = (temp.length() > 0 ? temp.charAt(0) : 0);
			if(answer == 'y' || answer == 'Y') {
				// Delete the existing file from the disk.
				DiskInterfaceUtils.deleteFile(newFileName);
			}
			else {
				System.out.println("File copy cancelled.");
				System.out.print("Press Enter to continue");
				Project3.scanner.nextLine();
				return;
			}
		}
		
		// Read in file bytes.
		try {
			byte[] fileContent = Files.readAllBytes(file.toPath());
			
			// Will the disk support the required file space?
			int requiredBlocks = (fileContent.length > 0 ? fileContent.length / (type == AllocationType.CHAINED ? CHAINED_BLOCK_SIZE : BLOCK_SIZE) : 0);
			if(requiredBlocks > 10) {
				System.out.println("The file is too large. Please try again with a smaller file.");
				return;
			}
			
			// Write the file to the disk based on Allocation Type.
			switch(type) {
			case CHAINED:
				DiskInterfaceUtils.writeChained(newFileName, fileContent, requiredBlocks);
				break;
			case INDEXED:
				DiskInterfaceUtils.writeIndexed(newFileName, fileContent, requiredBlocks);
				break;
			case CONTIGUOUS:
				DiskInterfaceUtils.writeContiguous(newFileName, fileContent, requiredBlocks);
				break;
			}
			
			// Sort the FAT after writing to disk.
			DiskInterfaceUtils.sortFAT();
		}
		catch (IOException e) {e.printStackTrace();}
		
		System.out.println("\nFile " + fileName + " copied");
		System.out.print("Press Enter to continue");
		Project3.scanner.nextLine();
	}
	
	/**
	 * Deletes a file from the disk by clearing all associated blocks and
	 * updates the file allocation table.
	 */
	private void deleteFile() {
		if(Project3.debugMode) System.out.println(getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "() called.");
		
		String fileName = "";
		
		// Update the FAT. Check for files.
		if(DiskInterfaceUtils.getFAT() > 0) {
			// Prompt for file name.
			while(!DiskInterfaceUtils.isValidFile(fileName)) {
				try {
					System.out.print("\nName of file: ");
					
					if(Project3.scanner.hasNextLine())
						fileName = Project3.scanner.nextLine().trim();
					
					if(fileName.equalsIgnoreCase("Exit")) return;
					
					if(fileName.length() > 4 && fileName.length() <= 8) break;
					else System.out.println("Invalid entry. Please enter a valid file name. E.g. \"text.txt\".");
				}
				catch(Exception e) {System.out.println("Invalid entry. Please try again.");}
			}
			
			// Confirm delete if the file have a valid index on the disk.
			if(DiskInterfaceUtils.getFileRecordIndex(fileName) >= 0) {
				System.out.print("\nAre you sure you want to delete " + fileName + " from the disk? [y/n]: ");
				String temp = Project3.scanner.nextLine();
				char answer = (temp.length() > 0 ? temp.charAt(0) : 0);
				if(answer == 'y' || answer == 'Y') {
					// Delete the file.
					DiskInterfaceUtils.deleteFile(fileName);
					
					// Print the confirmation message.
					System.out.println("\nThe file " + fileName + " has been deleted from the disk.");
				}
				else System.out.println("\nFile delete cancelled.");
			}
			// Otherwise, print the 'no such file on disk' message.
			else System.out.println("\nThe file " + fileName + " could not be found on the disk.");
		}
		// Print message if no files on disk.
		else System.out.println("\nThere are no files on the disk to delete.");
		
		System.out.print("Press Enter to continue");
		Project3.scanner.nextLine();
	}
	
	/**
	 * Prints the shutdown message and terminates the application.
	 */
	private void shutDown() {
		System.out.println("\nDisk is shutting down.");
		System.exit(0);
	}
}
