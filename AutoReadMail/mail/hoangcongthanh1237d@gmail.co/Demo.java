package experiment;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class Demo {
	public static void main(String[] args) {
		Set<Integer> set = new TreeSet<Integer>();
		set.add(1);
		set.add(2);
		set.add(3);
		
		set.forEach(name -> System.out.println(name));

		Iterator<Integer> iterator = set.iterator();
			while(iterator.hasNext()) {
				System.out.println(iterator.next());
			}
	}
}


