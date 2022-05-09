package fileBackedCollections;
/**
 *  FileBackedArrayList replaces an ArrayList with a method that allows
 *  storing a larger collection than can hold in memory.
 *  
 *  Matthew Clark  2021
 * 
 * 
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

public class FileBackedArrayList<E extends Serializable> 
	extends ArrayList<E> implements Serializable {
	
	private static final long serialVersionUID = -1057695858159751216L;
	private ArrayList<IndexEntry> index = new ArrayList<IndexEntry>();
	private File tempFile = null; // file object; could use just a String filename for this
	private RandomAccessFile file = null; // RAF object stored in tempFile
	private transient Random random = new Random();

	
	FileBackedArrayList() {
		boolean success = init();
		if (!success) {
			System.err.println("FileBackedArrayList: could not initialize file");
		}
	}
	
	/**
	 * create backing file.  The location of this file may have security implications
	 * so this wrapper could be changed to specify a more secure location
	 * @return temporary file
	 * @throws IOException
	 */
	File createBackingFile() throws IOException {
		File result =  File.createTempFile("filebackedcollection", "bin", null);
		result.deleteOnExit();
		return result;
	}
	
	
	/**
	 *  initialization of the storage file
	 *  @return true if the initialization seemed successful
	 * @throws NoSuchAlgorithmException 
	 */
	boolean init()  {
		
		if (tempFile == null && file == null) {
			try {
				tempFile = createBackingFile();
				file = new RandomAccessFile(tempFile, "rw");
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			} 
		}
		
		return true;
	}
	
	/**
	 * return the size of the backing file in bytes
	 * 
	 * @return long size of file
	 */
	public long fileSize() {
		
		try {
			if (file == null) {
				return 0;
			}
			synchronized(file) {
				return file.length();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	
	/**
	 * compact the data store file. Some operations like remove and set may make
	 * orphan objects in the file which may result in a lot of storage.  However,
	 * this operation may be disk/time intensive as it copies all of the objects 
	 * to a new file.
	 * 
	 */
	public void trim() {

		final FileBackedArrayList<E> update = new FileBackedArrayList<E>();

		for (E item : this) {
			update.add(item);
		}
		
		this.clear();
		
		index = update.index;
		file = update.file;
		tempFile = update.tempFile;
	}
	
	
	@Override
	public void clear() {
		// clear index
		index.clear();
		if (file != null) {
			try {
				synchronized(file) {
					file.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (tempFile != null && tempFile.exists()) {
			tempFile.delete();
		}
		tempFile = null;
		file = null;
		boolean success = init();
		if (!success) {
			System.err.println("clear: error initializing backing file");
		}
	}
	
	
	/**
	 * read an entry from the file and return the bytes
	 * @param entry serial number of object to get
	 * @return the object, serialized in bytes
	 * 
	 * @throws IOException
	 */
	private final byte[] read(int entry) throws IOException {
		
		final IndexEntry indx = index.get(entry);
		if (indx.getSize() == 0) return null;
		
		final byte[] objbytes = new byte[indx.getSize()];
		
		synchronized(file) {
			file.seek(indx.getPosition());       
			file.readFully(objbytes);
		}
		
		return encrypt(objbytes, indx.getSeed());
	}


	/**
	 * encrypt data using a combiner algorithm.  Each array has a different seed used for this XOR encryption.
	 * This is a medium level encryption designed to make the data stored in the file non-obvious way
	 * to thwart simple inspection. since the same seed is used for each object cryptographers could reverse engineer
	 * the seed for many arrays with modest effort.
	 * 
	 * @param data
	 * @return encrypted or decrypted data
	 */
	private final byte[] encrypt(final byte[] data, final long seed) {
		
		// reset the state, and modify the seed slightly by incorporating
		// the data size
		random.setSeed(seed);
		final byte[] randombytes = new byte[data.length];
		random.nextBytes(randombytes);
		
		for (int i = 0; i < data.length; i++) {
			data[i] ^= randombytes[i];
		}
		return data;
	}


	/**
	 * write a serialized object to the end of the memory mapped file as an array of bytes
	 * 
	 * @param bytes bytes to write
	 * @throws IOException
	 */
	private final void write(final byte[] bytes, long seed) throws IOException {
		
		if (bytes == null) {
			return;
		}
		
		boolean success = init();
		if (!success) {
			throw new IOException("Error creating backing file");
		}
		
		synchronized(file) {
			
			file.seek(file.length());
			file.write(encrypt(bytes, seed));
		}
	}
	


	/**
	 * serialize an object to a byte array
	 * 
	 * @param object Object to serialize
	 * @return byte array
	 * @throws IOException
	 */
	final private byte[] objectToBytes(Object object) throws IOException {
		
		if (object == null) {
			return null;
		}
		
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final ObjectOutputStream out = new ObjectOutputStream(bos);
		out.writeObject(object);
		out.close();
		return bos.toByteArray();
	}
	

	/**
	 * read bytes and return to an object
	 * @param bytes byte array from a serialized object
	 * @return the object that created the bytes
	 * @throws IOException
	 * @throws ClassNotFoundException if the object's class is not known
	 */
	final private E bytesToObject(final byte[] bytes) throws IOException, ClassNotFoundException {

		if (bytes == null || bytes.length == 0) {
			return null;
		}
		
		final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		final ObjectInputStream in = new ObjectInputStream(bis);
		@SuppressWarnings("unchecked")
		final E result = (E) in.readObject();
		in.close();
		return result;
	}

	@Override
	public int size() {
		return index.size();
	}


	/**
	 * remove the item at the specified index. this does not remove the actual data stored
	 * on disk however.
	 * 
	 * @param indx index of item to remove
	 * @return E item removed
	 */
	public E remove(int indx) {
		final E result = get(indx);
		index.remove(indx);
		return result;
	}

	
	/**
	 * get the item at the specified index
	 * 
	 * @param indx integer index
	 * @return E item at that index
	 */
	public E get(int indx) {
		
		if (indx < 0 || indx >= size()) {
			throw new IndexOutOfBoundsException(outOfBoundsMsg(indx));
		}
		
		try {
			IndexEntry ientry = index.get(indx);
			if (ientry.hashCode() == 0 || ientry.getSize() == 0 ) {
				return null;
			}
			return bytesToObject(read(indx));
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public boolean add(E item) {
		
		add(index.size(), item);
		return true;
	}
	
	/**
	 * add the item at the specified index, moving the indices of other items
	 * if necessary
	 * 
	 * @param indx integer index location
	 * @param E item to add
	 * @throws IndexOutOfBoundsException if index is < 0 or > size
	 * @throws NullPointerException if item is null
	 */
	public void add(int indx, E item) {
		
		if (indx < 0 || indx > size()) {
			throw new IndexOutOfBoundsException(outOfBoundsMsg(indx));
		}
		
		boolean success = init();
		if (!success) {
			throw new NullPointerException("add: error creating backing file");
		}

		try {
			
			final byte[] entry = objectToBytes(item);
			final IndexEntry idx = new IndexEntry(
					fileSize(), 
					entry != null ? entry.length : 0,
					item);
			
			write(entry, idx.getSeed());
			index.add(indx, idx);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	

	/**
	 * set the item at index 'indx', to 'item'. It writes the 
	 * new item to the end of the file,
	 * 
	 * @param indx integer index to set
	 * @param item E item to add to collection
	 * @return item replaced at the index location.
	 */
	public E set(int indx, E item) {
		
		if (indx < 0 || indx >= size()) {
			throw new IndexOutOfBoundsException(outOfBoundsMsg(indx));
		}
		
		E result = null;
		if (indx < size()) {
			result = get(indx);
		}
		
		try {
			final byte[] entry = objectToBytes(item);
			final int entry_len = entry != null ? entry.length : 0;
			
			
			final IndexEntry idx = new IndexEntry(
					fileSize(), 
					entry_len,
					item);
			
			write(entry, idx.getSeed());
			index.set(indx, idx);
		
			
		} catch (IOException e) {
			e.printStackTrace();
			
		}  catch (Exception e) {
			System.err.println("size is " + size());
			System.err.println("index " + indx + " item " + item + " " + index.get(indx));
			e.printStackTrace();
		}
		
		return  result;
	}

	/**
	 * return true if the collection is empty.
	 * 
	 * @return true if the collection is empty, false if not.
	 */
	public boolean isEmpty() {
		return size() == 0;
	}
	
	
	/**
	 * efficient indexOf does not require reading all objects from file,
	 * uses hash of object as an initial check before reading.
	 * 
	 * @param Object o to check index
	 * @returns int index in collection or -1 if not present.
	 */
	public int indexOf(Object o) {

		final int objectHash = (o == null ? 0 : o.hashCode());
		
		for (int i = 0; i < size(); i++) {
			final IndexEntry entry = index.get(i);
			if (entry.hashCode() == objectHash) {
				final E item = get(i);
				if (item.equals(o)) {
					return i;
				}
			}
		}
		return -1;
	}
	
	/**
	 * Returns true if the collection contains the argument object. 
	 * Uses hash code in index to check object to avoid reading through
	 * the entire file.
	 * 
	 * @param o object to check
	 * @return true if the object is contained in the collection
	 */
	public boolean contains(Object o) {
		
		return indexOf(o) != -1;
	}

	/**
	 * remove the object from the collection
	 * 
	 * @param Object o to remove
	 * @returns true if the object was removed
	 */
	public boolean remove(Object o) {
		
		final int indx = indexOf(o);
		if (indx != -1) {
			index.remove(indx);
			return true;
		}
		return false;
	}
	
/**
 * add all items from the argument collection to this collection
 * 
 * @param coll collection to add
 * @return true if this collection was changed
 */
	public boolean addAll(Collection<? extends E> coll) {
		
		boolean result = false;
		for (E item : coll) {
			result |= add(item);
		}
		return result;
	}
	
	/**
	 * retain only the items from the collection in this collection
	 * 
	 * @param coll items to keep
	 * @return true if the collection was changed
	 */
	public boolean retainAll(Collection<?> coll) {
		
		boolean result = false;
		
		for (Iterator<E> iter = this.iterator(); iter.hasNext();) {
			E item = iter.next();
			if (!coll.contains(item)) {
				iter.remove();
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Return true if this collections contains all of the objects in the argument
	 * 
	 * @param coll collection to compare to
	 * @return true if this collection contains all items in the argument
	 */
	public boolean containsAll(Collection<?> coll) {
		
		for (Object o : coll) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}
	
/**
 * Remove all objects in the collection from this collection.
 * 
 * @param col collection to remove
 * @return true if any object was removed
 */
	public boolean removeAll(Collection<?> col) {
		boolean result = false;
		
		for (Object o : col) {
			result |= remove(o);
		}
		
		return result;
	}

	/**
	 * Hashcode for this collection object
	 * compute hashcode without having to read each object file
	 * since the hashcodes are stored in the object
	 * 
	 * @return integer hashcode for collection
	 */
    public int hashCode() {
        int hashCode = 1;
        for (IndexEntry e : index)
            hashCode = 31*hashCode + e.hashCode();
        
        return hashCode;
    }
  
    
    /**
     * implement equals
     * 
     * @param Object o object to compare to
     * @return true if this collection has the same elements as the argument
     */
    public boolean equals(Object o) {
    	
    	if (o instanceof FileBackedArrayList ) {
    		final FileBackedArrayList<?> fbc = (FileBackedArrayList<?>) o;
    		if (fbc.size() != size() || fbc.hashCode() != hashCode() ) {
    			return false;
    		}
    		// here if he the sizes and hashcodes are equal, so the objects are probably equal
    		// but we check each object.
    		for (int i = 0; i < size(); i++) {
    			if (!index.get(i).equals(fbc.index.get(i))) {
    				return false;
    			}
    		}
    	}
    	return true;
    }
    
    private String outOfBoundsMsg(int index) {
        return "Out of bounds Index: "+index+", Size: "+size();
    }
    
    /*
     * (non-Javadoc)
     * try to clean up file if trash-collected. Windows has bugs affecting
     * deleting temporary files
     * 
     * @see java.lang.Object#finalize()
     */
    protected void finalize() {
    	
    	if (index != null) index.clear();
    	if (tempFile != null && tempFile.exists()) {
    		tempFile.delete();
    	}
    }

	@Override
	public Iterator<E> iterator() {
		Iterator<E> iter = new Iterator<E>() {
			
			private int index = 0;

			@Override
			public boolean hasNext() {
				return index < size();
			}

			@Override
			public E next() {
				E result = get(index++);
				return result;
			}
			
		};
		
		return iter;
	}
	
	
	
}