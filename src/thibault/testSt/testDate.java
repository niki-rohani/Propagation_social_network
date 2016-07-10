package thibault.testSt;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.la4j.factory.Basic1DFactory;
import org.la4j.inversion.GaussJordanInverter;
import org.la4j.inversion.MatrixInverter;
import org.la4j.matrix.Matrices;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.dense.Basic2DMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.dense.BasicVector;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

public class testDate {

	public static void main(String[] args) {
		
		
		
		
		/*DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);
		String time="2014-03-20T17:30:20Z";

			try {
				Date date = df.parse(time);
				System.out.println(date.getHours());
				long timestamp=(long) date.getTime()/1000;
				System.out.println(timestamp);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			Date d = new Date( (1351499192l)* 1000);
			
			System.out.println(d.getHours());
			
			
			int b=3;
			int c=4;
			System.out.println(b/c);
			
			//duree us: 10j11h25mn56s
		
		/*int sizeFeatures=5;
		Matrix Id=new Basic2DMatrix(new double[sizeFeatures][sizeFeatures] );
		Matrix A=Id;
		Vector v1,v2;
		//Matrix a=new Basic2DMatrix(new double[sizeFeatures][sizeFeatures] );
		
		for (int i = 0; i<sizeFeatures;i++){
			for (int j = 0; j<sizeFeatures;j++){
				if(i==j){Id.set(i, j, 1);}
				else{Id.set(i, j, 0);}
			}
		}	

		
		Basic1DFactory b = new Basic1DFactory();
		
		System.out.println(A);
		
		for (int i = 0; i<10;i++){
			v1=b.createRandomVector(sizeFeatures);
			v2=b.createRandomVector(sizeFeatures);
			A=A.add(v1.outerProduct(v1)).add(v2.outerProduct(v2));
			System.out.println(A);
			System.out.println(A.is(Matrices.POSITIVE_DEFINITE_MATRIX));
		}*/	
		
		
		/*for (int i = 0; i<sizeFeatures;i++){
			v.set(i,i+2);
		}	
		System.out.println(v.outerProduct(v));
		MatrixInverter inverter= new GaussJordanInverter(a) ;
		Matrix invA;
		invA=inverter.inverse();
		System.out.println(invA);*/
		
		
		String u = "a";
		int M=Integer.parseInt(u);
		}
	}


