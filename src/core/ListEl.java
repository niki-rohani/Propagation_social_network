package core;

public class ListEl<K,V> {
	private K k;
	private V v;
	private ListEl<K,V> next=null;
	public ListEl(K k,V v){
		this.k=k;
		this.v=v;
	}
	public K getK(){
		return(k);
	}
	public V getV(){
		return(v);
	}
	public void setNext(ListEl<K,V> l){
		this.next=l;
	}
	public ListEl<K,V> next(){
		return next;
	}
}
