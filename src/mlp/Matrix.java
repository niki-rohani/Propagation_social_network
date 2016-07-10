package mlp;
/**
 * This class describes a matrix.....
 */

public abstract class Matrix {

		protected int number_of_rows;
		protected int number_of_columns;	

		public Matrix(int _number_of_rows,int _number_of_columns)
		{
			number_of_rows=_number_of_rows;
			number_of_columns=_number_of_columns;		
		}

		/*public Matrix(int _number_of_rows,int _number_of_columns,float initial_value)
		{
			number_of_rows=_number_of_rows;
			number_of_columns=_number_of_columns;
		}*/
		
		public abstract String toString();

		public abstract void clear();
		
		//public abstract Matrix sample(int n);
		//public abstract Matrix getEmptyCopy(int nbr, int nbc);
		
		/**
		 * Returns the index of cell i,j in a matrix of ld rows.
		 * i = row, j = column, ld=number of rows
		 */
		public static int IDX2C(int i,int j,int ld){
			return(((j)*(ld))+(i)); 
		}
		
		/**
		 * Transform the matrix to the given number of rows and columns.
		 * Warning : after this call, values contained in the matrix may be not valid any more. 
		 * @param nbRows
		 * @param nbCols
		 */
		public abstract void transformTo(int nbRows,int nbCols);
		
		public int getNumberOfRows()
		{
			return(number_of_rows);
		}

		public int getNumberOfColumns()
		{
			return(number_of_columns);
		}

		public abstract void setValue(int r,int c,double v);
		public abstract double getValue(int r,int c);

		public abstract Matrix copy();	
		public abstract CPUMatrix copyToCPU();
		public abstract CPUSparseMatrix copyToCPUSparse();
		public abstract GPUMatrix copyToGPU();
		public abstract  void fill(double value);
		
		public long getMemorySize() {return(number_of_columns*number_of_rows*4);}

		/*public Matrix buildMatrixFromVector(float[] vals,int size)
		{
			CPUMatrix m=new CPUMatrix(1,size);
			for(int i=0;i<size;i++) m.setValue(0,i,vals[i]);
			return(m);
		}*/
}