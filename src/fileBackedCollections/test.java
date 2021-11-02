package fileBackedCollections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;


public class test {

	int NUM = 100000;
	Random rand = new Random();
	
	public static void main(String[] args) throws Throwable {
		new test().testArrayList();
		new test().testHashMap();
		new test().testSet();
		print("done");
	}
	
	static void print(String item) {
		System.out.println(item);
	}
	
	void testArrayList() {
		
		long start = System.currentTimeMillis();
		ArrayList<String> t = new FileBackedArrayList<String>();
		System.out.println("** test ArrayList with " + NUM);
		
		for (Integer i = 0; i < NUM; i++) {
			t.add("item " + i);
		}
		//print("item 100 is: " + t.get(100));
		
		print("after add() array length: " + t.size());
		t.clear();
		print("after clear() array length: " + t.size());
		
		for (Integer i = 0; i < NUM; i++) {
			t.add("item " + i);
			t.add(String.valueOf(i));
		}

		// test setting data
		String setvalue = "xxxxxx";
		for (int i = 0; i < NUM/3; i++) {
			t.set(i,setvalue + i);
		}
		for (int i = 0; i < NUM/3; i++) {
			assert t.get(i) == setvalue + i;
		}
		print("tested setting items");
		
		// test removing data
		for (int i = 0; i < NUM/3; i++) {
			t.remove(i);
		}
		print("after remove() array length:" + t.size());
		
		print("test null value in array slot 1");
		t.set(1, null);
		print("retrieved value: " + t.get(1));
		
		print("test arrayList with arrays as content");
		ArrayList<Long[]> u = new FileBackedArrayList<Long[]>();
		for (int i = 0; i < 10; i++) {
			Long[] L = new Long[10];
			for (int j = 0; j < 10; j++) {
				L[j] = rand.nextLong();
			}
			u.add(L);
		}
		u.add(new Long[5]);
		for (int i = 0; i < u.size(); i++) {
			Long[] x = u.get(i);
			String y = "array: ";
			for (int j = 0; j < x.length; j++) {
				y += x[j] + " ";
			}
			print(y);
		}
		
		System.out.println("took " + (System.currentTimeMillis() - start)/1000.0 + " ms");
		
	}
	
	void testSet() {
		
		System.out.println("** test Set");
		Set<String> t = new FileBackedSet<String>();
		
		for (int i = 0; i < NUM; i++) {
			t.add(String.valueOf(rand.nextInt(50000)));
		}
		print("adding int 1-100 set size is " + t.size());

	}
	void testHashMap() {
		
		print("** test HashMap");
		HashMap<String, String> t = new FileBackedHashMap<String, String>();

		for (int i = 0; i < NUM; i++) {
			t.put(String.valueOf(i), String.valueOf(i*9029349));
		}

		for (int i = 0; i < NUM; i++) {
			t.put(String.valueOf(i), String.valueOf(i*100));
		}		
		
		for (int i = 0; i < NUM; i++) {
			t.put(String.valueOf(i) + "x", String.valueOf(i*100));
		}		
		
		for (int i = 0; i < NUM/2; i++) {
			t.remove(String.valueOf(i) + "x");
		}	
		print("hash value 10x " + t.get("10x"));
		print("hash value 100 " + t.get("100"));
		t.put(null, "test null value");
		print("null value stored was " + t.get(null));
		print("end hash map size " + t.size());;
	}
	

}
