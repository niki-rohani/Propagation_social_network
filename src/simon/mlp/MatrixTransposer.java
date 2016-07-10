package simon.mlp;

import mlp.CPUMatrix;
import mlp.Matrix;

public class MatrixTransposer {

	public static CPUMatrix trans(Matrix m) {
		
		//System.err.println("Warning : MatrixTransposer could be faster...") ;
		CPUMatrix r = new CPUMatrix(m.getNumberOfColumns(), m.getNumberOfRows()) ;
		for(int i=0 ; i<m.getNumberOfRows() ; i++) {
			for(int j=0 ; j<m.getNumberOfColumns() ; j++) {
				r.setValue(j, i, m.getValue(i, j)) ;
			}
		}
		
		return r ;
	}
	
}
