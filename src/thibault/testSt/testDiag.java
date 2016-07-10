package thibault.testSt;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;

import org.la4j.decomposition.EigenDecompositor;
import org.la4j.inversion.GaussJordanInverter;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;

public class testDiag {

	public static void main(String[] args) {
		
		double[][] matTable = new double[][]{
				{1.4,2,3},
				{2,4,1},
				{3,1,6}};
		
		int d=4;
		double[] rep=new double[d];
		for(int j=0;j<d;j++){
			rep[j]=(Math.random()*Math.random()*10.0);
		}
		double[][] cov=new double[d][d];
		for(int j=0;j<d;j++){
			for(int l=0;l<d;l++){
				if(j==l)cov[j][l]=1+rep[j]*rep[j];
				else cov[j][l]=rep[j]*rep[l];
			}
		}
		
		
		
		Matrix M = new Basic2DMatrix(cov);
		
		EigenDecompositor eig = new EigenDecompositor(M);
		
		Matrix P = eig.decompose()[0];
		Matrix D = eig.decompose()[1];
		MatrixInverter inverter= new GaussJordanInverter(P);
		//Matrix invP = inverter.inverse();
		
		System.out.println(M);
		//System.out.println(P);
		//System.out.println(D);
		
		//System.out.println(invP);
		//System.out.println(P.transpose());
		
		//System.out.println(P.multiply(D).multiply(invP));
		System.out.println(P.multiply(D).multiply(P.transpose()));

		/*for(int i=0;i<d;i++){
			System.out.println(D.get(i, i));
		}
		
		try{
        	File f=new File("simParam.txt");
        	File dir=f.getParentFile();
        	if(dir!=null){
        		dir.mkdirs();
        	}
            PrintStream p = new PrintStream("simParam.txt") ;
            p.println("Beta:");
           p.println(M);
            
    		
            p.close();
		}
        catch(IOException e){
        	System.out.println("Probleme ecriture "+"simParam.txt");
        	
        }*/
		
		String  format = "dd.MM.yyyy_H.mm.ss";
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		String sdate=formater.format(date);
		String outputRep="simResults_"+sdate;
		File rep1=new File(outputRep);
		rep1.mkdirs();
		System.out.println(Paths.get(""));
		System.out.println(rep1.getPath());
		
		System.out.println((int)Math.ceil((2.02-1.0)/0.01));

	}

}
