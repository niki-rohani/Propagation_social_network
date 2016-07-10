package propagationModels;

import java.util.ArrayList;
import java.util.HashSet;





public class TransmissionProbaDifference{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static Double eval(ProbabilisticTransmissionModel ref,ProbabilisticTransmissionModel hyp) {
		HashSet<String> users=ref.getUsers();
		users.addAll(hyp.getUsers());
		ArrayList<String> lusers=new ArrayList<String>(users);
		double sum=0.0;
		System.out.println(lusers.size()*(lusers.size()-1)+" probas");
		for(int i=0;i<lusers.size();i++){
			String u=lusers.get(i);
			for(int j=i+1;j<lusers.size();j++){
				String v=lusers.get(j);
				Double pr=ref.getProba(u, v);
				Double ph=hyp.getProba(u, v);
				pr=(pr==null)?0:pr;
				ph=(ph==null)?0:ph;
				pr*=0.99998;
				pr+=0.00001;
				ph*=0.99998;
				ph+=0.00001;
				double d=Math.abs(pr-ph);
				d=d*d;
				
				
				sum+=d;
				System.out.println(sum+"\t"+d+"\t"+pr+"\t"+ph);
				
				pr=ref.getProba(v, u);
				ph=hyp.getProba(v, u);
				pr=(pr==null)?0:pr;
				ph=(ph==null)?0:ph;
				pr*=0.99998;
				pr+=0.00001;
				ph*=0.99998;
				ph+=0.00001;
				d=Math.abs(pr-ph); ///pr;
				d=d*d;
				sum+=d;
				System.out.println(sum+"\t"+d+"\t"+pr+"\t"+ph);
			}
		}
		return sum/(lusers.size()*(lusers.size()-1));
	}
	
	public static void main(String[] args){
		
		ArtificialModel ref=new ArtificialModel();
		ref.setModelFile("propagationModels/ArtificialModel_dim10_nMods1_db-artWWW_users-users_1_nbUsers100_linkThreshold1.0_cosineMode_meanMinDelay-0.0_meanVarDelay-0.0_maxT-100") ;	
		
		
		ProbabilisticTransmissionModel hyp=new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-artWWW_cascadesCol-"+args[0]+"_usersusers_1_linkThreshold1.0_asPos1_addNeg-0.0_unbiased-0.0_l1reg-0_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",0);
		Double e1=eval(ref,hyp);
		
		
		hyp=new CTIC("propagationModels/CTIC_step-1_ratioInits-1.0_nbMaxInits--1_db-artWWW_cascadesCol-"+args[0]+"_usersusers_1_linkThreshold1.0",0);
		Double e2=eval(ref,hyp);
		
		hyp=new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-artWWW_cascadesCol-"+args[0]+"_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-0_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0",0);
		Double e3=eval(ref,hyp);
		
		//hyp=new IC("propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-artWWW_cascadesCol-"+args[0]+"_usersusers_1_linkThreshold1.0_asPos-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-10.0_globalExtern-0.0_individualExtern-0.0",0);
		//Double e4=eval(ref,hyp);
		System.out.println("Erreur de hyp par rapport a ref :"+e1);
		System.out.println("Erreur de hyp par rapport a ref :"+e2);
		System.out.println("Erreur de hyp par rapport a ref :"+e3);
		//System.out.println("Erreur de hyp par rapport a ref :"+e4);
	}

}
