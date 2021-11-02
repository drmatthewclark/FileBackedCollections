package fileBackedCollections;
/**
 *  FileBackedSet replaces an Set with a method that allows
 *  storing a larger collection than can hold in memory.
 *  
 *  Matthew Clark  2021
 * 
 * 
 */

import java.io.Serializable;
import java.util.Set;

public class FileBackedSet<E extends Serializable> extends FileBackedArrayList<E>
	implements Set<E>, Serializable {

	private static final long serialVersionUID = -6302000297128005981L;

	/**
	 * implement add function to check to insure that the item is unique
	 */
	public boolean add(E o)  {
		if (contains(o)) {
			return false;
		}
		
		return super.add(o);
	}
}