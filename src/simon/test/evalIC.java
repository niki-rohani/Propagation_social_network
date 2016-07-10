package simon.test;

import propagationModels.IC;
import propagationModels.MLPproj;
import experiments.EvalPropagationModel;

public class evalIC {

	public static void main(String[] args) {
		
		
		if(args.length >0) {
			System.out.println("Usage : db testcascades testusers model resultsfile minprob(unused) maxiter nbsim likelyhoodeval?");
			String db = args[0] ;
			String testcascades = args[1] ;
			String testusers = args[2] ;
			String model = args[3] ;
			String resultFiles = args[4] ;
			Double minprob = Double.parseDouble(args[5]) ;
			int maxIter = Integer.parseInt(args[6]) ;
			int nbSim = Integer.parseInt(args[7]) ;
			int step = Integer.parseInt(args[8]) ;
			boolean likelyhoodeval = Boolean.parseBoolean(args[9]) ;

			if(!likelyhoodeval) {
				EvalPropagationModelConfig5 ev = new EvalPropagationModelConfig5(db, testcascades, testusers,step) ;
				ev.addModel(new IC(model,maxIter,2),nbSim);
				EvalPropagationModel.run(ev,resultFiles) ;
				return ;
			} else {
				EvalPropagationModelConfig5bis ev = new EvalPropagationModelConfig5bis(db, testcascades, testusers,step) ;
				ev.addModel(new IC(model,0,3),1);
				EvalPropagationModel.run(ev,resultFiles) ;
				return ;
			}
			
		}

	}

}
