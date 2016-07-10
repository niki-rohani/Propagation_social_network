package thibault.dynamicCollect;

import java.util.ArrayList;
import experiments.Result;
public class CollectEvalMeasureList extends CollectEvalMeasure {
     	protected ArrayList<CollectEvalMeasure> mes;
		public CollectEvalMeasureList(ArrayList<CollectEvalMeasure> mes){
			this.mes=mes;
		}
		public String getName(){
			String s=" List of Measures : ";
			for(CollectEvalMeasure m:mes){
				s+=m.getName()+" \t ";
			}
			return(s);
		}
		public Result eval(CollectRecorder recorder, long t){
			Result res=new Result(recorder.getModelName(),recorder.getRewardFunction().toString()+"_t="+t);
			for(CollectEvalMeasure m:mes){
				if(verbose>=2){
					System.out.println("Evaluation par "+m.getName());
				}
				Result r=m.eval(recorder,t);
				if(verbose>=1){
					System.out.println(r);
				}
				res.add(r);
			}
			
			return(res);
		}
	}
