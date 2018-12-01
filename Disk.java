

/**
 * Disk.java
 * 
 */
public class Disk {
	private int numBlocks;
	private int blockSize;
	private byte[][] disk;
	
	
	/**
	 * Constructs a Disk object with the {@code numBlocks} parameter for the
	 * number of blocks and the {@code blockSize} parameter for the size of each
	 * block.
	 * 
	 * @param   numBlocks
	 *          An {@code int} for the number of blocks on the Disk.
	 * @param   blockSize
	 *          An {@code int} for the number of bytes in each block.
	 */
	public Disk(int numBlocks, int blockSize) {
		this.numBlocks = numBlocks;
		this.blockSize = blockSize;
		setupDisk();
	}
	
	/**
	 * This method is called by the {@code Disk} object constructor and is used to
	 * instantiate the {@code byte} arrays.
	 */
	private void setupDisk() {
		disk = new byte[numBlocks][blockSize];
		for(byte[] row: disk)
			java.util.Arrays.fill(row, (byte) 0);
	}
	
	/**
	 * This is the function that is used to read from the {@code Disk}.
	 * 
	 * @param   block
	 *          An {@code int} for the block containing the {@code byte} to be read.
	 * @param   location
	 *          An {@code int} for the byte within the block to be read.
	 * @return  A {@code byte} value found in the indicated block and location on
	 *          the {@code Disk}.
	 */
	public byte read(int block, int location) {
		return disk[block][location];
	}
	
	/**
	 * This is the function that is used to write to the {@code Disk}.
	 * 
	 * @param   block
	 *          An {@code int} for the block containing the {@code byte} to be written.
	 * @param   location
	 *          An {@code int} for the byte within the block to be written.
	 * @param   value
	 *          A {@code byte} for the value to be written to the {@code Disk}.
	 */
	public void write(int block, int location, byte value) {
		disk[block][location] = value;
		for(int i = 0; i < blockSize; i++) {
			if(disk[block][i] != 0) {
				disk[1][block] = 1;
				return;
			}
		}
	}
}
