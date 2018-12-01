

/**
 * FileRecord.java
 * 
 */
public class FileRecord {
	private String fileName;
	private int block, length;
	
	/**
	 * This is the default constructor for a {@code FileRecord} object.
	 */
	public FileRecord() {
		setFileName("");
		setBlock(-1);
		setLength(0);
	}
	
	/**
	 * This is an overloaded constructor for a {@code FileRecord} object in a
	 * chained or contiguous file allocation type.
	 * 
	 * @param   fileName
	 *          A {@code String} value for the name of the file.
	 * @param   startBlock
	 *          An {@code int} value representing the starting block location on
	 *          the disk.
	 * @param   length
	 *          An {@code int} value representing the required number of blocks to
	 *          store the given file for preallocation.
	 */
	public FileRecord(String fileName, int startBlock, int length) {
		setFileName(fileName);
		setBlock(startBlock);
		setLength(length);
	}
	
	/**
	 * This is an overloaded constructor for a {@code FileRecord} object in an
	 * indexed file allocation type.
	 * 
	 * @param   fileName
	 *          A {@code String} value for the name of the file.
	 * @param   indexBlock
	 *          An {@code int} value representing the index block location on the
	 *          disk.
	 */
	public FileRecord(String fileName, int indexBlock) {
		setFileName(fileName);
		setBlock(indexBlock);
		setLength(0);
	}
	
	/**
	 * This is the copy constructor for a {@code FileRecord} object.
	 * 
	 * @param   f
	 *          A {@code FileRecord} containing the source {@code FileRecord} that is
	 *          being copied to this {@code FileRecord} object.
	 */
	public FileRecord(FileRecord f) {
		setFileName(f.getFileName());
		setBlock(f.getStartBlock());
		setLength(f.getLength());
	}
	
	// Setter procedures.
	private void setFileName(String val)	{fileName = new String(val);}
	private void setBlock(int val)			{block = val;}
	private void setLength(int val)			{length = val;}
	
	// Getter functions.
	public String getFileName()				{return new String(fileName);}
	public int getStartBlock()				{return block;}
	public int getIndexBlock()				{return block;}
	public int getLength()					{return length;}
	
	/**
	 * The {@code FileRecord} equals function override for the {@code Object} class.
	 * 
	 * @param   o
	 *          An {@code Object} containing a {@code FileRecord} object for comparison
	 *          with this {@code FileRecord} object.
	 *          
	 * @see     java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		// If the object is compared with itself then return true
		if(o == this) return true;
		
		/* Check if o is an instance of FileRecord or not
		 * "null instanceof [type]" also returns false */
		if(!(o instanceof FileRecord)) return false;
		
		// typecast o to FileRecord so that we can compare data members
		FileRecord r = (FileRecord) o;
		
		// Compare the data members and return accordingly
		return fileName.compareTo(r.fileName) == 0;
	}
	
	/**
	 * The {@code FileRecord} compareTo function override for the {@code Object} class.
	 * <p>
	 * Returns 0 if the argument {@code FileRecord} is equal to this {@code FileRecord} or
	 * if the name of the argument {@code FileRecord} equals the name of this
	 * {@code FileRecord}; a value less than 0 if this {@code FileRecord} name string is
	 * lexicographically less than the {@code FileRecord} name string argument;
	 * and a value greater than 0 if this {@code FileRecord} name string is
	 * lexicographically greater than the {@code FileRecord} name string argument.
	 * 
	 * @param   r
	 *          A FileRecord to be compared with this FileRecord.
	 * @return  Returns 0 if the argument {@code FileRecord} is equal to this
	 *          {@code FileRecord} or if the name of the argument {@code FileRecord}
	 *          equals the name of this {@code FileRecord}; a value less than 0 if
	 *          this {@code FileRecord} name string is lexicographically less than
	 *          the {@code FileRecord} name string argument; and a value greater
	 *          than 0 if this {@code FileRecord} name string is lexicographically
	 *          greater than the {@code FileRecord} name string argument.
	 * 
	 * @see     java.lang.String#compareTo(java.lang.String)
	 */
	public int compareTo(FileRecord r) {
		if(this.equals(r)) return 0;
		else return this.fileName.compareTo(r.fileName);
	}
	
	/**
	 * The FileRecord toString function override for the Object class.
	 * <p>
	 * Returns a string representation for the FileRecord object.
	 * 
	 * @return  Returns a {@code String} representation for the {@code FileRecord} object.
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if(length > 0) return String.format("FileName: \"%s\", Start Block: %d, Length: %d", fileName, block, length);
		else return String.format("FileName: \"%s\", Index Block: %d", fileName, block);
	}
}
