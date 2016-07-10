package simon.tools;

import org.jblas.*;

public class testJblas {

	public static void main(String args[]) {	
		DoubleMatrix m = new DoubleMatrix(3,3,1, 2, 1,2,4,2,1,2,3) ;
		m.print();
		
		DoubleMatrix d = m.mmul(m.transpose()) ;
		d.print();
		DoubleMatrix eig[] =Eigen.symmetricEigenvectors(d) ;
		
		DoubleMatrix eigVect = eig[0] ;
		DoubleMatrix eigVal = eig[1] ;
		eigVal.print();
		eigVal.put(0, 1, Math.sqrt(eigVal.get(1, 1))) ;
		eigVal.put(2, 2, Math.sqrt(eigVal.get(2, 2))) ;
		eigVal.put(0, 0, Math.sqrt(eigVal.get(0, 0))) ;
		
		DoubleMatrix m2 = eigVect.mmul(eigVal) ;
		m2.print();
	}
	
	
}
