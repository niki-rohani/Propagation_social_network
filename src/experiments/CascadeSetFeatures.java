package experiments;
import java.util.ArrayList;
import java.util.HashMap;

import utils.Keyboard;

import actionsBD.MongoDB;

import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import java.util.HashSet;
import core.ArrayListStruct;
import core.Structure;
import cascades.*;
public class CascadeSetFeatures  implements Experiment {
	ArrayList<CascadeSetFeatureProducer> featurers;
	
	public CascadeSetFeatures(ArrayList<CascadeSetFeatureProducer> featurers){
		this.featurers=featurers;
	}
	public Result go(Structure struct){
		ArrayListStruct<String> acas=(ArrayListStruct<String>)struct;
		String db=acas.get(0);
		String col=acas.get(1);
		HashSet<Cascade> cascades=Cascade.getCascadesFromDB(db, col);
		//System.out.println(cascade);
		Result res=new Result(this.getDescription(),db+"."+col);
		ArrayList<Double> features;
		for(CascadeSetFeatureProducer featurer:featurers){
			System.out.println(featurer);
			features=featurer.getFeatures(cascades);
			int i=1;
			
			for(Double d:features){
				String name=featurer.toString();
				if(features.size()>1){
					name+="_"+i;
				}
				res.addScore(name,d);
				i++;
			}
		}
		return(res);
	}
	
	public String getDescription(){
		return("CascadeSetFeatures");
	}
	
	public static void main(String[] args){
		
		ArrayList<CascadeSetFeatureProducer> featurers=new ArrayList<CascadeSetFeatureProducer>();
		featurers.add(new NbCascadesPerUser());
		featurers.add(new NbCommonCascadesPerPairUsers());
		featurers.add(new NbPostsPerCascade());
		ResultFile rf=new ResultFile("res_CascadeSetFeatures.txt");
		CascadeSetFeatures cf=new CascadeSetFeatures(featurers);
		ArrayListStruct<String> a=new ArrayListStruct<String>();
		a.add(args[0]);
		a.add(args[1]);
		Result r=cf.go(a);
		try{
			rf.append(r);
		}
		catch(Exception e){
			System.out.println(e);
		}
		
		
	}
}