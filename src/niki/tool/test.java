package niki.tool;

import org.la4j.vector.sparse.CompressedVector;

public class test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CompressedVector v = new CompressedVector(1000);
		v.set(1, 1.0);
		v.set(2, 3.0);
		CompressedVector v2 = new CompressedVector(1000);
		v2.set(1, 1.0);
		v2.set(2, 3.0);
		
		System.out.println (v.innerProduct(v2)/(v.norm()*v2.norm()));
	}

}
