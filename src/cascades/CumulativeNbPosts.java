package cascades;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;
import optimization.Fonction;
import optimization.InferFonctionFactory;
import optimization.LossFonctionFactory;
import optimization.Optimizer;
import optimization.OptimizerFactory;
import optimization.Parameters;
import optimization.ParametrizedModel;
import utils.Keyboard;

public class CumulativeNbPosts extends CascadeFeatureProducer {
	private boolean normalize;
	private HashMap<Long,Step> steps;
	private long maxid;
	private long minid;
	private long step;
	//private int minStep; // minStep to consider
	
	/*public CumulativeNbPosts(){
		this(1);
	}
	public CumulativeNbPosts(long step){
		this(step,"","");
	}*/
	
	public CumulativeNbPosts(String db,String colStep){
		this(db,colStep,false);
	}
	public CumulativeNbPosts(String db,String colStep,boolean normalize){
		this(Step.loadSteps(db,colStep),normalize);
	}
	public CumulativeNbPosts(HashMap<Long,Step> steps,boolean normalize){
		this.steps=steps;
		this.normalize=normalize;
		this.step=0;
		if(steps.size()>0){
			step=steps.values().iterator().next().getStep();
		}
		minid=-1;
		maxid=-1;
		//this.minStep=minStep;
		for(Long id:steps.keySet()){
		  //if (id>=minStep){
			if ((minid==-1) || (id<minid)){
				minid=id;
			}
			if ((maxid==-1) || (id>maxid)){
				maxid=id;
			}
		  //}
		}
		
	}
	public ArrayList<Double> getFeatures(Cascade cascade){
		
		ArrayList<Double> ret=new ArrayList<Double>();
		HashSet<Post> cposts=cascade.getPosts();
		if(cposts.size()>0){
			long debut=-1;
			long fin=-1;
			HashMap<Long,Integer> nbs=new HashMap<Long,Integer>();
			/*for(Integer i:steps.keySet()){
				nbs.put(i, 0);
			}*/
			for(Post p:cposts){
				long t=Step.getIdStep(p.getTimeStamp(),step);
				//if (t>=this.minStep){
					if((debut<0) || (t<debut)){
						debut=t;
					}
					if((fin<0) || (t>fin)){
						fin=t;
					}
					int n=1;
					if (nbs.containsKey(t)){
						n+=nbs.get(t);
					}
					nbs.put(t, n);
				//}
			}
			long start=debut-10;//debut-1-cposts.size();
			long end=fin+10; //fin+1+cposts.size();
			for(long i=start;i<debut;i++){
				nbs.put(i, 0);
			}
			for(long i=fin+1;i<=end;i++){
				nbs.put(i, 0);
			}
			/*for(int i=start;i<=end;i++){
				if (!nbs.containsKey(i)){
					nbs.put(i, 0);
				}
			}*/
			//debut--;
			//fin++;
			//nbs.put(0, 0);
			//nbs.put(debut, 0);
			//nbs.put(fin, 0);
			//nbs.put(fin*2, 0);
			//debut=debut-step;
			//debut=Step.getIdStep(debut, step);
			HashMap<Long,Double> cumul=new HashMap<Long,Double>(); 
			ArrayList<Long> times=new ArrayList<Long>(nbs.keySet());
			Collections.sort(times);
			double sum=0.0;
			
			//int ctime=debut;
			//int cn=0;
			for(long t:times){
				double plus=nbs.get(t)*1.0;
				if (normalize){
					//BasicDBObject query=new BasicDBObject();
					//query.put("id", t);
					//DBObject res=col.findOne(query);
					Step sp=steps.get(t); //Double.valueOf(res.get("nbPosts").toString());
					if ((sp!=null) && (sp.getNbPosts()>0)){
						plus/=(1+Math.log(sp.getNbPosts()));
						//plus/=sp.getNbPosts();
					}
				}
				sum+=plus;
				cumul.put(t, sum);
			}
			//cn++;
			//cumul.put(cn, sum);
			ArrayList<HashMap<Integer,Double>> samples=new ArrayList<HashMap<Integer,Double>>();
			ArrayList<Double> labels=new ArrayList<Double>();
			HashMap<Integer,Double> h;
			for(Long i:times){ //int i=0; i<cn; i++){
				double n=cumul.get(i);
				h=new HashMap<Integer,Double>();
				//double sp=i-fin+(fin-debut)/2.0;
				double sp=Math.log(i);
				//double sp=i/(fin*1.0);
				//System.out.println(i+"=>"+sp);
				h.put(1, sp);
				samples.add(h);
				double val=(n*1.0);
				if(sum>0){
					val/=sum;
				}
				labels.add(val);
			}
			//double val=0.0;
			InferFonctionFactory infact=new InferFonctionFactory(3);
			Fonction f=infact.buildFonction();
			ParametrizedModel m=new ParametrizedModel(f);
			Optimizer opt=(new OptimizerFactory(1)).buildOptimizer();
			Fonction loss=(new LossFonctionFactory(1)).buildFonction();
			Parameters par=m.getParams();
			HashMap<Integer,Double> pa=new HashMap<Integer,Double>();
			pa.put(0, -1.0);
			pa.put(1, 1.0);
			par.setParams(pa);
			m.learn(samples,labels,loss,opt,0.0000000000000001,100000,1000);
			System.out.println("Infered values");
			ArrayList<Double> il=m.infer(samples);
			for(int i=0;i<labels.size();i++){
				HashMap<Integer,Double> samp=samples.get(i);
				double x=samp.values().iterator().next();
				double iv=il.get(i);
				double vv=labels.get(i);
				System.out.println(Math.exp(x)+" \t "+iv+" \t "+vv);
			}
			Fonction f2=f.getReverseParamsSamplesFonction();
			Fonction f3=f2.getDerivativeFonction();
			//Fonction min=infact.buildMinus(f3);
			
			
			System.out.println("Infered values");
			par=new Parameters();
			pa=new HashMap<Integer,Double>();
			double max=0.0;
			for(HashMap<Integer,Double> sample:samples){
				par.clearParameters();
				int tstep=0;
				double vtstep=0.0;
				for(Integer x:sample.keySet()){
					//System.out.println(x);
					vtstep=sample.get(x);
					pa.put(1,vtstep);
					//tstep=x;
					
				}
				par.setParams(pa);
				f3.setParams(par);
				double infval=f3.getValue();
				System.out.print(vtstep+"="+infval+";");
				if(infval>max){
					max=infval;
				}
			}
			System.out.println("");
			
			//opt.optimize(min, 0.000001, 1000,500);
			System.out.println("max ="+max);
			
			ret.add(max);
		}
		else{
			ret.add(0.0);
		}
		
		return(ret);
	}
	
	public String toString(){
		return("CumulativeNbPosts"+step);
	}
}
