package mlp;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Iterator;

import jcuda.Sizeof;
import jcuda.driver.CUdeviceptr;
import jcuda.runtime.JCuda;
import jcuda.runtime.cudaError;
public class CPUSparseMatrix extends Matrix {
	protected TreeMap<Integer,Double> values;
	protected Iterator<Map.Entry<Integer,Double>> iterator;
	public CPUSparseMatrix(int _number_of_rows,int _number_of_columns){
		super(_number_of_rows, _number_of_columns);
		values=new TreeMap<Integer,Double>();
	}
	
	public void clear(){
		values.clear();
	}
	
	public String toString()
	{
		StringBuilder sb=new StringBuilder();
		sb.append("Matrix (" + number_of_rows + ";" + number_of_columns + ") = " +"\n");
		for(Map.Entry<Integer, Double> e:values.entrySet()){
			int f=e.getKey();
			double v=e.getValue();
			int row=f%number_of_rows;
			int col=f/number_of_rows;
			sb.append("(" + row + ";" + col + ")" + ":" + v + " ");
		}
		sb.append("\n");
		return(sb.toString());
	}
	
	public void initIterator()
	{	
		iterator=values.entrySet().iterator();	
	}
	

	public boolean hasNextCell()
	{
		if(iterator==null){
			initIterator();
		}
		return(iterator.hasNext());
	}

	public Cell nextCell()
	{
		Map.Entry<Integer, Double> c=iterator.next();
		double val=c.getValue();
		int row=c.getKey()%number_of_rows;
		int col=c.getKey()/number_of_rows;
		Cell cell=new Cell(row,col,val);
		return(cell);
	}
	
	
	@Override
	 public Matrix copy()
	 {
			CPUSparseMatrix m=new CPUSparseMatrix(number_of_rows,number_of_columns);
			int row; int col; double val;
			for(Map.Entry<Integer, Double> e:values.entrySet()){
				m.values.put(e.getKey(),e.getValue());
			}

			return(m);
	}
	 
	@Override
	public CPUMatrix copyToCPU()
	{
		System.out.println("Warning:copying sparse Matrix to CPU Dense...");
		CPUMatrix retour=new CPUMatrix(number_of_rows,number_of_columns);
		retour.fill(0.0f);
		
		for(Map.Entry<Integer, Double> e:values.entrySet()){
			int row=e.getKey()%number_of_rows;
			int col=e.getKey()/number_of_rows;
			retour.setValue(row,col,e.getValue());
		}		
		return(retour);
	}
	
	@Override
	public GPUMatrix copyToGPU()
	{
		System.out.println("Warning:copying sparse Matrix to GPU Dense...");
		CPUMatrix m=copyToCPU();		
		return(m.copyToGPU());
	}

	@Override
	public CPUSparseMatrix copyToCPUSparse()
	 { 
		  return((CPUSparseMatrix)copy());
	 }
	
	@Override
	public void fill(double value) {
		if (value==0)
		{
			values=new TreeMap<Integer,Double>();
		}
		else
		{
			System.out.println("WARNING: Filling a sparse matrix with a non-null value");
			for(int r=0;r<number_of_rows;r++)
				for(int c=0;c<number_of_columns;c++)
				{
					setValue(r,c,value);
				}
		}
	}
	
	@Override
	public void setValue(int r, int c, double v) {
		if(r>=this.number_of_rows){
			throw new RuntimeException("CPUSparseMatrix.setValue => The matrix does not contain a row "+r+" (only "+this.number_of_rows+" rows)");
		}
		if(c>=this.number_of_columns){
			throw new RuntimeException("CPUSparseMatrix.setValue => The matrix does not contain a column "+c+" (only "+this.number_of_columns+" columns)");
		}
		values.put(IDX2C(r,c,number_of_rows),v);

	}
	/**
	 * Transform the matrix to the given number of rows and columns.
	 * Warning : if numbers of cols or rows have changed, this call builds a new empty values array  
	 * @param nbRows
	 * @param nbCols
	 */
	 public void transformTo(int nbRows,int nbCols){
		if((nbRows!=number_of_rows) || (nbCols!=number_of_columns)){
			this.number_of_columns=nbCols;
			this.number_of_rows=nbRows;
			values=new TreeMap<Integer,Double>();
			iterator=null;
		}
	}

	public void setValues(TreeMap<Integer,Double> vals){
		values=vals;
	}
	
	@Override
	public double getValue(int r, int c) {
		if(r>=this.number_of_rows){
			throw new RuntimeException("CPUSparseMatrix.getValue => The matrix does not contain a row "+r+" (only "+this.number_of_rows+" rows)");
		}
		if(c>=this.number_of_columns){
			throw new RuntimeException("CPUSparseMatrix.getValue => The matrix does not contain a column "+c+" (only "+this.number_of_columns+" columns)");
		}
		int i=IDX2C(r,c,number_of_rows);
		Double v=values.get(i);
		v=(v==null)?0.0f:v;		
		return(v);
	}

	public void addValue(int r,int c,double v)
	 {
		int i=IDX2C(r,c,number_of_rows);
		Double val=values.get(i);
		val=(val==null)?0.0f:val;
		values.put(i,val+v);
	 }

}


class Cell{
	int i;
	int j;
	double v;
	public Cell(int i,int j, double v){
		this.i=i;
		this.j=j;
		this.v=v;
	}
}