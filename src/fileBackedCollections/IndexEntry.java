package fileBackedCollections;
/**
 * index for arraylist objects held in the filebackedarraylist system.
 * 
 */
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * index for the memory mapped file with starting positions and sizes of the objects in the file
 * @author clarkm
 *
 */

class IndexEntry implements Serializable {
	
	private static final long serialVersionUID = 6628455650099912122L;
	private long position;
	private int size;
	private int objectHash;
	private long seed;
	private static SecureRandom seeder = null;
	

	/**
	 * constructor for index entry
	 * 
	 * @param position - position in file
	 * @param size  - size of object in bytes
	 * @param object - object
	 */
	IndexEntry(long position, int size, Object object) {
		
		if (seeder == null) {
			try {
				seeder = SecureRandom.getInstanceStrong();
			} catch (Exception e) {
				System.err.println("IndexEntry:" + e);
				return;
			}
		}
		this.position = position;
		this.size = size;
		objectHash = Objects.hashCode(object);
		this.seed = seeder.nextLong();
	}
	
	/**
	 * return encryption key for this entry
	 * @return long key to encrypt this object
	 */
	public long getSeed() {
		return seed;
	}
	/**
	 * return size of object
	 * @return integer size
	 */
	public int getSize() {
		return size;
	}
	
	/**
	 * get the file position of the object
	 * 
	 * @return long offset into file
	 */
	public long getPosition() {
		return position;
	}
	
	/**
	 * get the object hash
	 * 
	 * @return int hash of object
	 */
	public int hashCode() {
		return objectHash;
	}
	

	/**
	 * override equals method
	 * 
	 * @return true if the object is equal to this, false otherwise
	 */
	public boolean equals(final Object o) {

		if (o instanceof IndexEntry) {
			IndexEntry obj = (IndexEntry)o;
			
			return  position == obj.position && 
					size == obj.size &&
					hashCode() == obj.hashCode();
		}
		return false;
	}
}
