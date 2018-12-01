

/**
 * AllocationType.java
 * An enumeration for disk allocation types.
 * 
 */
public enum AllocationType {
	
	/**
	 * Key used by DiskInterface to specify chained file allocation type.
	 */
	CHAINED(0),
	
	/**
	 * Key used by DiskInterface to specify indexed file allocation type.
	 */
	INDEXED(1),
	
	/**
	 * Key used by DiskInterface to specify contiguous file allocation
	 * type.
	 */
	CONTIGUOUS(2);
	
	/**
	 * A constant holding the value for the {@code AllocationType}.
     */
    private final int allocationType;
	
	/**
	 * Constructs an allocation type with the {@code allocationType}
	 * property set to the given value.
	 */
	AllocationType(int allocationType) {
		this.allocationType = allocationType;
	}
	
	/**
	 * Returns the {@code allocationType} value for this AllocationType.
	 */
	int getAllocationType() {
		return allocationType;
	}
}
