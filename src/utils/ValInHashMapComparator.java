package utils;

import java.util.Comparator;

import java.util.HashMap;
import java.util.HashSet;

public class ValInHashMapComparator<T,V extends Comparable<V>> implements Comparator<T>
{
		HashMap<T,V> h;
		boolean dec=false;
		public ValInHashMapComparator(HashMap<T,V> h){
			this(h,false);
		}
		public ValInHashMapComparator(HashMap<T,V> h,boolean dec){
			this.h=h;
			this.dec=dec;
		}
		public int compare(T un,T deux){
			V x=h.get(un);
			V y=h.get(deux);
			int c=x.compareTo(y);
			if(dec){
				if(c==-1) c=1;
				else if(c==1) c=-1;
			}
			return c;
		}
	}