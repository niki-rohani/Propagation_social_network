package mlp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import jcuda.runtime.JCuda;
import jcuda.runtime.cudaMemcpyKind;
import jcuda.*;

public class CPUMatrix extends Matrix {

		protected double[] values;
		//protected boolean memory_allocated_outside;

		public CPUMatrix(int _number_of_rows,int _number_of_columns){
			this(_number_of_rows, _number_of_columns, 0);
		}
		
		public CPUMatrix(int _number_of_rows,int _number_of_columns,double initial_value) 
		{
		    super(_number_of_rows,_number_of_columns);
			values=new double[number_of_rows*number_of_columns];
			
			for(int i=0;i<number_of_rows*number_of_columns;i++)
			{
				values[i]=initial_value;
			}
			//memory_allocated_outside=false;
		}

		public CPUMatrix(ArrayList<Double> vals){
			super(1,vals.size());
			values=new double[number_of_columns];
			
			for(int i=0;i<number_of_columns;i++)
			{
				values[i]=vals.get(i);
			}
		}
		
		public void clear(){
			values=new double[values.length];
		}
		
		public void fill(double value)
		{
			for(int i=0;i<number_of_rows*number_of_columns;i++)
			{
				values[i]=value;
			}
		}


		public String toString()
		{
			StringBuilder sb=new StringBuilder();
			sb.append("CPU Matrix ("+ number_of_rows + ";" + number_of_columns + ") = \n");
			System.out.println();
			for(int r=0;r<number_of_rows;r++)
			{
				for(int c=0;c<number_of_columns;c++)
				{
					sb.append(values[IDX2C(r,c,number_of_rows)]+"; " );
				}
				sb.append(" \n");
			}
			return(sb.toString());
		}

		public double[] getValues() {return(values);}
		
		
			
		

		public Matrix copy()
		{
			CPUMatrix m=new CPUMatrix(number_of_rows,number_of_columns);
			for(int i=0;i<number_of_rows*number_of_columns;i++)
			{
				m.values[i]=values[i];
			}
			return(m);
		}

		public CPUMatrix copyToCPU()
		{
			return((CPUMatrix)copy());
		}
		
		
		public GPUMatrix copyToGPU()
		{
			GPUMatrix m=new GPUMatrix(number_of_rows,number_of_columns);
			Pointer v=m.getValues();
			JCuda.cudaMemcpy(v,Pointer.to(values),number_of_rows*number_of_columns*Sizeof.DOUBLE,cudaMemcpyKind.cudaMemcpyHostToDevice);
			return(m);
		}
		
		
		
		

		public CPUSparseMatrix copyToCPUSparse()
		{
			CPUSparseMatrix retour=new CPUSparseMatrix(number_of_rows,number_of_columns);
			for(int r=0;r<number_of_rows;r++)
			{
				for(int c=0;c<number_of_columns;c++)
				{
					retour.setValue(r,c,getValue(r,c));
				}
			}
			return(retour);
		}

		public void setValue(int r,int c,double v)
		{
			if(r>=this.number_of_rows){
				throw new RuntimeException("CPUMatrix.setValue => The matrix does not contain a row "+r+" (only "+this.number_of_rows+" rows)");
			}
			if(c>=this.number_of_columns){
				throw new RuntimeException("CPUMatrix.setValue => The matrix does not contain a column "+c+" (only "+this.number_of_columns+" columns)");
			}
			values[IDX2C(r,c,number_of_rows)]=v;
		}
		
		public void setValues(double v)
		{
			for(int i=0;i<values.length;i++){
				values[i]=v;
			}
		}

		public double getValue(int r,int c)
		{
			if(r>=this.number_of_rows){
				throw new RuntimeException("CPUMatrix.getValue => The matrix does not contain a row "+r+" (only "+this.number_of_rows+" rows)");
			}
			if(c>=this.number_of_columns){
				throw new RuntimeException("CPUMatrix.getValue => The matrix does not contain a column "+c+" (only "+this.number_of_columns+" columns)");
			}
			return(values[IDX2C(r,c,number_of_rows)]);
		}
		
		
		/**
		 * Transform the matrix to the given number of rows and columns.
		 * Warning : after this call, values contained in the matrix may be not valid any more. 
		 * @param nbRows
		 * @param nbCols
		 */
		public void transformTo(int nbRows,int nbCols){
			if(nbRows*nbCols>values.length){
				values=new double[nbRows*nbCols];
				//System.out.println("transform to "+nbRows+","+nbCols);
			}
			this.number_of_columns=nbCols;
			this.number_of_rows=nbRows;
		}
	
		
}
