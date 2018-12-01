import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;


/**
 * DiskInterfaceUtils.java
 * 
 */
public class DiskInterfaceUtils extends DiskInterface {
	
	/**
	 * Constructs the {@code DistInterface} object with the
	 * {@code AllocationType} set to the given value.
	 * 
	 * @param   type
	 * 	        A disk {@code AllocationType}.
	 * @throws  Exception 
	 *          If the {@code BLOCK_SIZE} is less than the
	 *          {@code NUM_BLOCKS}.
	 * @see     DiskInterface
	 * @see     AllocationType
	 */
	public DiskInterfaceUtils(AllocationType type) throws Exception {
		super(type);
	}

	/**
	 * Returns {@code true} if the file is in the file allocation table; otherwise
	 * {@code false}.
	 * 
	 * @param   fileName
	 *          A {@code String} value for the name of the file.
	 * @return  Returns {@code true} if the file is in the file allocation table;
	 *          otherwise {@code false}.
	 */
	protected static boolean isValidFile(String fileName) {
		getFAT();
		for(int i = 0; i < fileAllocationTable.size(); i++)
			if(fileName.trim().equalsIgnoreCase(fileAllocationTable.get(i).getFileName().trim())) return true;
		return false;
	}
	
	/**
	 * Updates the local copy of the file allocation table and returns the
	 * number of contained files.
	 * 
	 * @return  Returns an {@code int} value representing the number of files
	 *          recorded in the file allocation table.
	 */
	protected static int getFAT() {
		if(Project3.debugMode) System.out.println(new Object(){}.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "() called.");
		
		int numRecords = 0;
		
		// Get the number of stored files.
		for(int i = 0; i < BLOCK_SIZE; i += recordSize)
			if(disk.read(0, i) != 0) numRecords++;
		
		// Create a new, emply table for the file data.
		fileAllocationTable.clear();
		
		// Iterate through the records to add them to the table.
		for(int i = 0; i < maxRecords; i++) {
			if(Project3.debugMode) System.out.println("Reading from block " + i + ", byte starting at " + i * recordSize + ".");
			
			// Iterate by record size.
			int startByte = i * recordSize;
			
			// Check for non-null byte in the first record byte index.
			if(disk.read(0, startByte) != (byte) 0) {
				// Get/Store the file name.
				byte[] tempName = new byte[8];
				for(int j = 0; j < 8; j++) {
					if(disk.read(0, startByte + j) == 0) break;
					tempName[j] = disk.read(0, startByte + j);
					if(Project3.debugMode) System.out.println("0: " + disk.read(0, startByte + j));
				}
				
				if(new String(tempName).trim().equals("")) break;
				fileAllocationTable.add(new FileRecord(new String(tempName), Byte.toUnsignedInt(disk.read(0, startByte + 8)), Byte.toUnsignedInt(disk.read(0, startByte + 9))));
			}
		}
		
		return numRecords;
	}
	
	/**
	 * Returns a {@code FileRecord} object if found by the given file name;
	 * otherwise {@code null}.
	 * 
	 * @param   fileName
	 *          A {@code String} value representing the name of the file to
	 *          locate.
	 * @return  Returns a {@code FileRecord} object if found by the given file
	 *          name; otherwise {@code null}.
	 * @see     FileRecord
	 */
	protected static FileRecord getFileRecord(String fileName) {
		// Update the FAT.
		getFAT();
		
		FileRecord fileRecord = null;
		
		for(FileRecord f : fileAllocationTable) {
			if(fileName.trim().equalsIgnoreCase(f.getFileName().trim())) {
				fileRecord = new FileRecord(f);
				break;
			}
		}
		
		return fileRecord;
	}
	
	/**
	 * Returns an {@code int} value representing the disk index if the file is
	 * located by the given file name; otherwise -1.
	 * 
	 * @param   fileName
	 *          A {@code String} value representing the name of the file to
	 *          locate.
	 * @return  Returns an {@code int} value representing the disk index if the
	 *          file is located by the given file name; otherwise -1.
	 */
	protected static int getFileRecordIndex(String fileName) {
		// Iterate the FAT to locate the named record. Return the index of the record.
		for(int i = 0; i < fileAllocationTable.size(); i++)
			if(fileName.equalsIgnoreCase(fileAllocationTable.get(i).getFileName().trim()))
				return i;
		return -1;
	}
	
	/**
	 * Writes the content of the given file content as a byte[] to the
	 * disk using the chained disk allocation type. After the content has
	 * been written, the file allocation table is updated with the file
	 * name, starting block index, and block length.
	 * 
	 * @param   fileName
	 *          A {@code String} value representing the name of the file.
	 * @param   fileContent
	 *          A {@code byte[]} object containing the original file content to be
	 *          stored.
	 * @param   requiredBlocks
	 *          An {@code int} value representing the required number of blocks to
	 *          store the given file for preallocation.
	 */
	protected static void writeChained(String fileName, byte[] fileContent, int requiredBlocks) {
		// Make sure there is space available.
		if(getNumFreeBlocks() >= requiredBlocks) {
			// Get a random starting block.
			int startBlock = getRandomBlock();
			int block = startBlock;
			int blocksWritten = 0;
			ArrayList<Integer> usedBlocks = new ArrayList<Integer>();
			usedBlocks.add(startBlock);
			
			// Add the first block to the file space bitmap.
			disk.write(1, startBlock, (byte) 1);
			
			// Iterate the file to write.
			for(int i = 0; i < fileContent.length; i++) {
				if(Project3.debugMode) System.out.println(i + ": " + block);
				
				if(i > 0 && i % CHAINED_BLOCK_SIZE == 0) {
					block = getRandomBlock();
					usedBlocks.add(block);
					blocksWritten++;
					disk.write(block, i - (blocksWritten * CHAINED_BLOCK_SIZE), fileContent[i]);
				}
				else
					disk.write(block, i - (blocksWritten * CHAINED_BLOCK_SIZE), fileContent[i]);
			}
			
			if(Project3.debugMode) for(int i : usedBlocks) System.out.println("Block Used: " + i);
			
			// Add the chain links and update the file bitmap.
			for(int i = 1; i < usedBlocks.size(); i++) {
				if(Project3.debugMode) System.out.println("Used Block: " + i);
				disk.write(usedBlocks.get(i - 1), CHAINED_BLOCK_SIZE, (byte) ((int)usedBlocks.get(i)));
				disk.write(1, usedBlocks.get(i), (byte) 1);
			}
			
			// Update the FAT.
			writeFileToFAT(fileName, startBlock, requiredBlocks);
		}
	}
	
	/**
	 * Writes the content of the given file content as a byte[] to the
	 * disk using the indexed disk allocation type. After the content has
	 * been written, the file allocation table is updated with the file
	 * name and index block.
	 * 
	 * @param   fileName
	 *          A {@code String} value representing the name of the file.
	 * @param   fileContent
	 *          A {@code byte[]} object containing the original file content to be
	 *          stored.
	 * @param   requiredBlocks
	 *          An {@code int} value representing the required number of blocks to
	 *          store the given file for preallocation.
	 */
	protected static void writeIndexed(String fileName, byte[] fileContent, int requiredBlocks) {
		// Make sure there is space available.
		if(getNumFreeBlocks() >= requiredBlocks) {
			ArrayList<Integer> usedBlocks = new ArrayList<Integer>();
			
			// Get a random index block.
			int indexBlock = getRandomBlock();
			
			// Update the bitmap for the index block. 
			disk.write(1, indexBlock, (byte)1);
			
			// Get a random starting block and verify the block hasn't been used.
			int startBlock = getRandomBlock();
			usedBlocks.add(startBlock);
			int block = startBlock;
			int blocksWritten = 0;
			
			// Iterate the file to write.
			for(int i = 0; i < fileContent.length; i++) {
				if(Project3.debugMode) System.out.println(i + ": " + block);
				
				if(i > 0 && i % BLOCK_SIZE == 0) {
					block = getRandomBlock();
					usedBlocks.add(block);
					blocksWritten++;
					disk.write(block, i - (blocksWritten * BLOCK_SIZE), fileContent[i]);
				}
				else
					disk.write(block, i - (blocksWritten * BLOCK_SIZE), fileContent[i]);
			}
			
			// Add the index block and update the file bitmap.
			for(int i = 0; i < usedBlocks.size(); i++) {
				if(Project3.debugMode) System.out.println("Used Block: " + i);
				disk.write(indexBlock, i, (byte) ((int)usedBlocks.get(i)));
				disk.write(1, usedBlocks.get(i), (byte) 1);
			}
			
			writeFileToFAT(fileName, indexBlock, requiredBlocks);
		}
	}
	
	/**
	 * Writes the content of the given file content as a byte[] to the
	 * disk using the contiguous disk allocation type. After the content
	 * has been written, the file allocation table is updated with the
	 * file name, starting block index, and block length.
	 * 
	 * @param   fileName
	 *          A {@code String} value representing the name of the file.
	 * @param   fileContent
	 *          A {@code byte[]} object containing the original file content to be
	 *          stored.
	 * @param   requiredBlocks
	 *          An {@code int} value representing the required number of blocks to
	 *          store the given file for preallocation.
	 */
	protected static void writeContiguous(String fileName, byte[] fileContent, int requiredBlocks) {
		// Make sure there is space available.
		int startBlock = getNextContigSpace(requiredBlocks);
		if(startBlock >= 2) {
			int block = startBlock;
			disk.write(1, startBlock, (byte) 1);
			int blocksWritten = 0;
			
			// Iterate the file to write.
			for(int i = 0; i < fileContent.length; i++) {
				if(Project3.debugMode) System.out.println(i + ": " + block);
				
				if(i > 0 && i % BLOCK_SIZE == 0) {
					block++;
					disk.write(1, block, (byte) 1);
					blocksWritten++;
					disk.write(block, i - (blocksWritten * BLOCK_SIZE), fileContent[i]);
				}
				else
					disk.write(block, i - (blocksWritten * BLOCK_SIZE), fileContent[i]);
			}
			
			// Update the file bitmap.
			for(int i = startBlock; i < startBlock + requiredBlocks; i++) {
				if(Project3.debugMode) System.out.println("Used Block: " + i);
				disk.write(1, i, (byte) 1);
			}
			
			writeFileToFAT(fileName, startBlock, requiredBlocks);
		}
	}
	
	/**
	 * Updates the file allocation table and the free space bitmap once
	 * the file content has been written to the disk.
	 * 
	 * @param   fileName
	 *          A {@code String} value representing the name of the file.
	 * @param   block
	 *          An {@code int} value representing the starting block or index
	 *          block location on the disk.
	 * @param   length
	 *          An {@code int} value representing the required number of blocks to
	 *          store the given file for preallocation.
	 */
	private static void writeFileToFAT(String fileName, int block, int length) {
		int currRecords = DiskInterfaceUtils.getFAT();
		
		// Convert the file name String to bytes and write them to the FAT.
		for(int i = 0; i < fileName.length(); i++)
			disk.write(0, (currRecords * recordSize) + i, fileName.getBytes()[i]);
		
		// Write the strating/index block to the FAT.
		disk.write(0, (currRecords * recordSize) + 8, (byte)block);
		
		// Store the number of blocks to the FAT.
		if(type != AllocationType.INDEXED) disk.write(0, (currRecords * recordSize) + 9, (byte)length);
		
		// Update the File System Bitmap.
		updateBitmap();
	}
	
	/**
	 * A utility function used to quickly get the number of empty blocks
	 * on the disk.
	 * 
	 * @return  An {@code int} value representing the number of empty blocks on
	 *          the disk.
	 */
	private static int getNumFreeBlocks() {
		int freeBlocks = 0;
		for(int i = 2; i < NUM_BLOCKS; i++) {
			if(isBlockEmpty(i)) freeBlocks++;
		}
		return freeBlocks;
	}
	
	/**
	 * A utility function used to quickly get an empty block from the disk.
	 * 
	 * @return  An {@code int} value representing the index of the empty block.
	 */
	private static int getRandomBlock() {
		if(getNumFreeBlocks() <= 0) return -1;
		
		int block = -1;
		while(true) {
			block = (int)(Math.random() * (NUM_BLOCKS - 2) + 2);
			if(isBlockEmpty(block)) break;
		}
		return block;
	}
	
	/**
	 * A utility function used to quickly get the next available
	 * contiguous space that is empty and large enough to store the
	 * specified number of needed blocks. Returns the index of the first
	 * block of the contiguous space; otherwise -1.
	 * 
	 * @param   needed
	 *          An {@code int} value representing the necessary number of empty,
	 *          contiguous blocks.
	 * @return  An {@code int} value representing the index of the first block of
	 *          the contiguous space; otherwise -1.
	 */
	private static int getNextContigSpace(int needed) {
		int freeBlocks = 0;
		for(int i = 2; i < NUM_BLOCKS - 1; i++) {
			if(isBlockEmpty(i)) {
				if(freeBlocks == 0) freeBlocks++;
				if(isBlockEmpty(i + 1)) freeBlocks++;
				else freeBlocks = 0;
				if(freeBlocks >= needed) return i - freeBlocks + 2;
			}
		}
		return -1;
	}
	
	/**
	 * Checks the given block by index and returns {@code true} if it is empty;
	 * otherwise {@code false}.
	 * 
	 * @param   block
	 *          An {@code int} value representing the block index on the disk to
	 *          check.
	 * @return  Returns {@code true} if the given block by index is empty;
	 *          otherwise {@code false}.
	 */
	private static boolean isBlockEmpty(int block) {
		for(int i = 0; i < BLOCK_SIZE; i++)
			if(disk.read(block, i) != 0) return false;
		return true;
	}
	
	/**
	 * Returns the contents of the given file as a {@code byte[]}.
	 * 
	 * @param   fileName
	 *          A {@code String} value representing the name of the file.
	 * @return  A {@code byte[]} containing the contents of the file on the disk.
	 */
	protected static byte[] getFileBytesFromDisk(String fileName) {
		if(Project3.debugMode) System.out.println(new Object(){}.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "() called.");
		
		ArrayList<Byte> fileContent = new ArrayList<Byte>();
		
		// Get file info from FAT.
		FileRecord fileRecord = DiskInterfaceUtils.getFileRecord(fileName);
		int block = 0, length = 0;
		
		// Get file bytes based on allocation type.
		switch(type) {
		case CHAINED:
			// FAT Chained: File Name, Start Block, Length
			block = fileRecord.getStartBlock();
			length = fileRecord.getLength();
			
			// Chained: Random free block. Last bit points to next free block.
			while(true) {
				if(length > 1) {
					for(int i = 0; i < CHAINED_BLOCK_SIZE; i++)
						fileContent.add(disk.read(block, i));
					length--;
					block = (disk.read(block, CHAINED_BLOCK_SIZE) & 0xFF);
				}
				else {
					for(int i = 0; i < BLOCK_SIZE; i++)
						fileContent.add(disk.read(block, i));
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
			
			// Indexed: Random Free block to store an ordered list of other random free blocks holding the file.
			while(true) {
				block = indexes.remove(0);
				
				if(length > 1) {
					for(int i = 0; i < BLOCK_SIZE; i++)
						fileContent.add(disk.read(block, i));
					length--;
				}
				else {
					for(int i = 0; i < BLOCK_SIZE ; i++)
						fileContent.add(disk.read(block, i));
					break;
				}
			}
			
			break;
		case CONTIGUOUS:
			// FAT Contiguous: File Name, Start Block, Length
			block = fileRecord.getStartBlock();
			length = fileRecord.getLength();
			
			// Contiguous: First available set of blocks large enough.
			while(true) {
				if(length > 1) {
					for(int i = 0; i < BLOCK_SIZE; i++)
						fileContent.add(disk.read(block, i));
					block++;
					length--;
				}
				else {
					for(int i = 0; i < BLOCK_SIZE ; i++)
						fileContent.add(disk.read(block, i));
					break;
				}
			}
			
			break;
		}
		
		// Remove all trailing null values from the block.
		for(int i = fileContent.size() - 1; i >= 0; i--) {
			if(fileContent.get(i) == 0) fileContent.remove(i);
			else break;
		}
		
		// Convert the content to a byte[].
		byte[] tempFileContent = new byte[fileContent.size()];
		for(int i = 0; i < fileContent.size(); i++) tempFileContent[i] = fileContent.get(i);
		
		// Return the byte[] object.
		return tempFileContent;
	}
	
	/**
	 * Deletes the contents of the given file from the disk.
	 * 
	 * @param   fileName
	 *          A {@code String} value representing the name of the file.
	 */
	protected static void deleteFile(String fileName) {
		if(Project3.debugMode) System.out.println(new Object(){}.getClass().getName() + "." + new Object(){}.getClass().getEnclosingMethod().getName() + "() called.");
		
		// Get file info from FAT.
		FileRecord fileRecord = DiskInterfaceUtils.getFileRecord(fileName);
		int fileRecordIndex = DiskInterfaceUtils.getFileRecordIndex(fileName);
		int block = 0, length = 0;
		
		// Get file bytes based on allocation type.
		switch(type) {
		case CHAINED:
			// FAT Chained: File Name, Start Block, Length
			block = fileRecord.getStartBlock();
			length = fileRecord.getLength();
			
			// Chained: Random free block. Last bit points to next free block. Allow for consolidation.
			while(true) {
				if(length > 1) {
					// Save the next block location.
					int nextBlock = (disk.read(block, CHAINED_BLOCK_SIZE) & 0xFF);
					
					// Clear the contents of the current block.
					clearBlock(block);
					
					// Decrement chain length.
					length--;
					
					// Move to the next chained block.
					block = nextBlock;
				}
				else {
					// Clear the contents of the current block.
					clearBlock(block);
					
					// Remove the file from the FAT.
					for(int i = 0; i < recordSize; i++)
						disk.write(0, (fileRecordIndex * recordSize) + i, (byte)0);
					
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
			
			// Indexed: Random Free block to store an ordered list of other random free blocks holding the file.
			while(true) {
				// Get the location for the current block.
				block = indexes.remove(0);
				
				if(length > 1) {
					// Clear the contents of the current block.
					clearBlock(block);
					
					// Decrement index length.
					length--;
				}
				else {
					// Clear the contents of the current block.
					clearBlock(block);
					
					// Clear the index block.
					clearBlock(fileRecord.getIndexBlock());
					
					// Remove the file from the FAT.
					for(int i = 0; i < recordSize; i++)
						disk.write(0, (fileRecordIndex * recordSize) + i, (byte)0);
					
					break;
				}
			}
			break;
		case CONTIGUOUS:
			// FAT Contiguous: File Name, Start Block, Length
			block = fileRecord.getStartBlock();
			length = fileRecord.getLength();
			
			// Contiguous: First available set of blocks large enough. Allow for compaction.
			for(int i = 0; i < length; i++)
				clearBlock(block + i);
			
			// Remove the file from the FAT.
			for(int i = 0; i < recordSize; i++)
				disk.write(0, (fileRecordIndex * recordSize) + i, (byte)0);
			
			break;
		}
		
		// Sort the FAT.
		sortFAT();
	}
	
	/**
	 * A utility function used to quickly clear all of the bytes in a
	 * given block by index.
	 * 
	 * @param   block
	 *          An {@code int} value representing the block index on the disk to clear.
	 */
	private static void clearBlock(int block) {
		disk.write(1, block, (byte) 0);
		for(int i = 0; i < BLOCK_SIZE; i++)
			disk.write(block, i, (byte) 0);
	}
	
	/**
	 * A utility function used to quickly retrieve, sort, and re-write the
	 * files in the disk file allocation table and then update the free
	 * space bitmap.
	 */
	protected static void sortFAT() {
		int numRecords = 0;
		
		// Get the number of valid files in the FAT.
		for(int i = 0; i < maxRecords; i++)
			if(disk.read(0, i * recordSize) != 0) numRecords++;
		
		String[][] tempFileAllocationTable = new String[numRecords][3];
		
		// If one record, move it to the top of the FAT.
		if(numRecords == 1) {
			int index = 0;
			
			for(int i = 0; i < maxRecords; i++) if(disk.read(0, i * recordSize) != 0) index = i;
			
			if(index > 0)
				for(int j = 0; j < recordSize; j++) {
					disk.write(0, j, disk.read(0, (index * recordSize) + j));
					disk.write(0, (index * recordSize) + j, (byte) 0);
				}
		}
		// Otherwise, get the records and sort.
		else {
			int counter = 0;
			// Store the current, unsorted FAT to a temporary table.
			for(int i = 0; i < maxRecords; i++) {
				if(Project3.debugMode) System.out.println("Reading from block " + i + ", byte starting at " + i * recordSize + ".");
				int startByte = i * recordSize;
				
				if(disk.read(0, startByte) != 0) {
					byte[] tempName = new byte[8];
					for(int j = 0; j < 8; j++) {
						if(disk.read(0, startByte + j) == 0) break;
						tempName[j] = disk.read(0, startByte + j);
					}
					tempFileAllocationTable[counter][0] = new String(tempName);
					if(tempFileAllocationTable[counter][0].trim().equals("")) break;
					
					tempFileAllocationTable[counter][1] = Integer.toString(Byte.toUnsignedInt(disk.read(0, startByte + 8)));
					
					tempFileAllocationTable[counter++][2] = (type == AllocationType.INDEXED ? null : Integer.toString(Byte.toUnsignedInt(disk.read(0, startByte + 9))));
				}
			}
			
			// Sort the current, unsorted FAT in the temporary table.
			if(numRecords > 1) {
				Arrays.sort(tempFileAllocationTable, new Comparator<String[]>(){
					@Override
					public int compare(String[] first, String[] second){
						// compare the first element
						int comparedTo = first[0].compareTo(second[0]);
						// if the first element is same (result is 0), compare the second element
						if(comparedTo == 0) return first[1].compareTo(second[1]);
						else return comparedTo;
					}
				});
			}
			
			// Clear the current FAT.
			clearBlock(0);
			
			// Write the new, sorted FAT to disk.
			for(int i = 0; i < numRecords; i++) {
				if(Project3.debugMode) System.out.println("Reading from block " + i + ", byte starting at " + i * recordSize + ".");
				int startByte = i * recordSize;
				
				for(int j = 0; j < 8; j++)
					disk.write(0, startByte + j, tempFileAllocationTable[i][0].getBytes()[j]);
				
				disk.write(0, startByte + 8, (byte) Integer.parseInt(tempFileAllocationTable[i][1]));
				
				if(type != AllocationType.INDEXED)
					disk.write(0, startByte + 9, (byte) Integer.parseInt(tempFileAllocationTable[i][2]));
			}
		}
		
		// Update the File System Bitmap.
		updateBitmap();
	}
	
	/**
	 * A utility function used to quickly scan the disk for used and free
	 * blocks before updating the disk bitmap block.
	 */
	protected static void updateBitmap() {
		int usedBlocks = 0;
		for(int i = 2; i < NUM_BLOCKS; i++) {
			if(!isBlockEmpty(i)) {
				disk.write(1, i, (byte) 1);
				usedBlocks++;
			}
			else
				disk.write(1, i, (byte) 0);
		}
		
		if(usedBlocks > 0) {
			disk.write(1, 0, (byte) 1);
			disk.write(1, 1, (byte) 1);
		}
		else {
			disk.write(1, 0, (byte) 0);
			disk.write(1, 1, (byte) 0);
		}
	}
	
	/**
	 * A purpose-built console user selection function with data
	 * validation.
	 * <p>
	 * This function takes the given header and options arrays and
	 * presents the user with a formatted interface with the listed
	 * options for selection. The function loops through the prompt until
	 * a valid selection is made by the user based on the given options.
	 * 
	 * @param   header
	 *          A {@code String[]} containing the lines found in the header area.
	 * @param   options
	 *          A {@code String[]} containing the lines found in the options area.
	 * @param   leftPad
	 *          An {@code int} value that is used to add additional leading spaces
	 *          to the number portion of the options section for alignment
	 *          purposes.
	 * @return  An {@code int} value representing the prompt number selected by
	 *          the user.
	 */
	protected static int optionChooser(String[] header, String[] options, int leftPad) {
		if(header == null || header.length <= 0 || options == null || options.length <= 0) return '0';
		
		int choice = 0;
		
		System.out.println("\n**********************************************************************");
		System.out.println("***                                                                ***");
		System.out.printf("%-" + ((70 - header[0].length())/2) + "s%s%" + ((70 - header[0].length())/2 + (((70 - header[0].length())%2)==1 ?  1: 0)) + "s\n", "***", header[0], "***");
		
		if(header.length > 1)
			for(int i = 1; i < header.length; i++) {
				System.out.printf("%-" + ((70 - header[0].length())/2 + 4) + "s%s%" + ((70 - header[i].length())/2 + (((70 - header[i].length())%2)==1 ?  1: 0) - 4 + ((70 - header[i].length())/2 - (70 - header[0].length())/2)) + "s\n", "***", header[i], "***");
			}
		
		System.out.println("***                                                                ***");
		System.out.println("**********************************************************************");
		System.out.println();
		
		if(options.length > 0)
			for(int i = 0; i < options.length; i++) {
				System.out.printf("%" + (leftPad >= 0 ? leftPad : 0) + "d. %s\n", i + 1, options[i]);
			}
		
		while(true) {
			try {
				System.out.print("\nType in your option: ");
				
				if(Project3.scanner.hasNextLine())
					choice = Integer.parseInt(Project3.scanner.nextLine().trim());
				
				if(choice > 0 && choice <= options.length) break;
				else
					System.out.println("Invalid entry. Please enter a number from 1 to " + options.length + ".");
			}
			catch(Exception e) {
				System.out.println("Invalid entry. Please try again.");
			}
		}
		return choice;
	}
}
