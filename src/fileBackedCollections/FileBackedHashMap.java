package fileBackedCollections;
/**
 *  FileBackedHashMap replaces an HashMap with a method that allows
 *  storing a larger collection than can hold in memory.
 *  
 *  Matthew Clark  2021
 * 
 * 
 */

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class FileBackedHashMap<K,V extends Serializable> extends HashMap<K, V> 
 implements Serializable {

	/**
	 * serial version id
	 */
	private static final long serialVersionUID = -5655605924486711518L;
	/**
	 *  the index is held in memory, while the data is stored
	 * in the FileBackedAbstractList. This hashmap stores the connection
	 * between the key and which index of the file backed array it 
	 * corresponds to.
	*/
	private HashMap<K, Integer> index = new HashMap<K, Integer>();
	
	// store for the values of the hash map, the index of the hash entries
	// is stored as the value in the "index" hashmap.
	private FileBackedArrayList<V> storage = new FileBackedArrayList<V>();
	/**
	 * hashmaps can store items with a null key.  this is a special case
	 * bucket
	 */
	V nullValue = null;  // value for null key.
	/**
	 * put item into hashmap.  either replace an existing value or
	 * add a new value for the key
	 * 
	 * @Param key K key for hash
	 * @param value V object for item.
	 * 
	 */
	public V put(K key, V value) {
		
		if (key == null) {   // special case store in the special bucket
			nullValue = value;
			
		} else if (index.containsKey(key)) {
			int storageIndex = index.get(key);
			storage.set(storageIndex, value);
			
		} else {
			int storageIndex = storage.size(); // last array index
			index.put(key, storageIndex);
			storage.add(storageIndex, value);
		}

		return value;
	}

	/**
	 * get object from hashmap
	 * 
	 * @param Object key - key for hashmap
	 * @return V object from hashmap
	 */
	public V get(Object key) {
		
		if (key == null) return nullValue; // get from special bucket
		
		final Integer oid = index.get(key);
		if (oid == null) {
			return null;
		}
		return (V) storage.get(oid);
	}
	
	/**
	 * check existence of this key
	 * @param key Object key to check
	 * @return boolean true if the object is in the map
	 */
	public boolean containsKey(Object key) {
		return index.containsKey(key);
		
	}
	
	/**
	 * check existence of value in the map, for any key
	 * the value may be under multiple keys.
	 * 
	 * @param value  value to check for
	 * @return true if the value is in the map
	 */
	public boolean containsValue(Object value) {
		return storage.contains(value);
	}
	
	/**
	 * This returns a file backed set of the values of the hashmap.
	 * 
	 */
	public Set<V> values() {

		final FileBackedSet<V> result = new FileBackedSet<V>();
		for (V value : storage) {
			result.add(value);
		}
		return result;
	}
	
	/**
	 * @override
	 * returns the keyset for this hashmap
	 * @return set of keys
	 */
	public Set<K> keySet() {
		return index.keySet();
	}
	
	
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		
		HashSet<Entry<K,V>> result = new HashSet<Entry<K,V>>();
		for (K key : index.keySet()) {
			Entry<K,V> map = new AbstractMap.SimpleEntry<K, V>(key, get(key));
			result.add(map);
		}
		return result;
	}
	
 	public void putAll(Map<? extends K,? extends V> m) {
 		for (java.util.Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
 			put(entry.getKey(), entry.getValue());
 		}
 	}
 	
 	public V remove(Object key) {
 		V result = get(key);
 		index.remove(key);
 		return result;
 	}
 	
 	/**
 	 * return length of storage file
 	 * @return long file size
 	 */
 	public long fileSize() {
 		return storage.fileSize();
 	}
 }
