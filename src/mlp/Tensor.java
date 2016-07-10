package mlp;

public class Tensor {
	public static int nbTensors=0;
	public int id;
	protected int number_of_matrices;
	protected Matrix[] matrices;
	//protected Singleton *singleton;
		
	public Tensor(int _number_of_matrices)
	{
			number_of_matrices=_number_of_matrices;
			matrices=new Matrix[number_of_matrices];
			nbTensors++;
			id=nbTensors;
			//singleton=0;
	}

	public Tensor(Matrix m)
	{
			number_of_matrices=1;
			matrices=new Matrix[1];
			matrices[0]=m;
			//singleton=0;
			nbTensors++;
			id=nbTensors;
	}

	public Tensor(Matrix m1,Matrix m2)
	{
			number_of_matrices=2;
			matrices=new Matrix[2];
			matrices[0]=m1;
			matrices[1]=m2;
			//singleton=0;
			nbTensors++;
			id=nbTensors;
	}


	public void setMatrix(int i,Matrix m)
	{
			matrices[i]=m;
	}

	public Matrix getMatrix(int i)
	{
			return(matrices[i]);
	}

	public long getMemorySize() 
	{
		long m=0;
		for(int i=0;i<matrices.length;i++)
				m+=matrices[i].getMemorySize();
		
		return(m);
	}

	public int getNumberOfMatrices() {return(number_of_matrices);}

	public void ensureCPUMatrices()
	{
		//if (singleton!=0) singleton->declareGPUTensor(this);
		for(int i=0;i<number_of_matrices;i++)
		{
			if (matrices[i]!=null)
			{
				Matrix m=matrices[i];
				if(!(m instanceof CPUMatrix)){
					//if (Singleton::getDisplayCPUGPUCopies()) 
					System.out.println(" -> CPU pour ["+ m.getNumberOfRows()+ ";"+ m.getNumberOfColumns()+ "]");
					CPUMatrix nm=matrices[i].copyToCPU();
					matrices[i]=nm;
				}			
			}
		}			
	}	
	
	public void ensureGPUMatrices(){
		//if (singleton!=0) singleton->declareGPUTensor(this);
				for(int i=0;i<number_of_matrices;i++)
				{
					if (matrices[i]!=null)
					{
						Matrix m=matrices[i];
						if(!(m instanceof GPUMatrix)){
							//if (Singleton::getDisplayCPUGPUCopies()) 
							System.out.println(" -> GPU pour ["+ m.getNumberOfRows()+ ";"+ m.getNumberOfColumns()+ "]");
							GPUMatrix nm=matrices[i].copyToGPU();
							matrices[i]=nm;
						}			
					}
				}	
	}
	public void ensureCPUSparseMatrices(){
		//if (singleton!=0) singleton->declareGPUTensor(this);
		for(int i=0;i<number_of_matrices;i++)
		{
			if (matrices[i]!=null)
			{
				Matrix m=matrices[i];
				if(!(m instanceof CPUSparseMatrix)){
					//if (Singleton::getDisplayCPUGPUCopies()) 
					System.out.println(" -> CPUSparse pour ["+ m.getNumberOfRows()+ ";"+ m.getNumberOfColumns()+ "]");
					CPUSparseMatrix nm=matrices[i].copyToCPUSparse();
					matrices[i]=nm;
				}			
			}
		}
	}

	/*	void setSingleton(Singleton *s)
		{
			singleton=s;
		}
	 */
		/*Tensor *copyCPU()
		{
			Tensor *t=new Tensor(number_of_matrices);
			for(int i=0;i<number_of_matrices;i++)
			{
				t->setMatrix(i,matrices[i]->copyCPU());
			}
			return(t);
		}*/

	public String toString()
	{
			StringBuilder sb=new StringBuilder();
			sb.append("Tensor of size id="+id + matrices.length + "\n");
			for(int i=0;i<matrices.length;i++)
			{
				sb.append("\tMatrix " + i + "= \n");
				sb.append(matrices[i]);
			}
			return(sb.toString());
	}
	
}
