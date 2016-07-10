package core;

public class Element<T> {
	private T val;
	
	public Element(T t){
		val=t;
	}
	public T getVal(){
		return(val);
	}
	public int hashCode(){
		return(val.hashCode());
	}

}
