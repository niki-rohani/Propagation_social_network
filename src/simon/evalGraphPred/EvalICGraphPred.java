package simon.evalGraphPred;

import java.util.HashMap;

import propagationModels.IC;
import propagationModels.MLPproj;
import core.User;

public class EvalICGraphPred {
	
	String modelfil ="propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC-1/usersusers_1_linkThreshold1.0_contaMaxDelay-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0"; //"propagationModels/ICmodel_step1_ratioInits-1.0_nbMaxInits--1_db-weibo_cascadesCol-cascades_1_start1_nbC30000/usersusers_1_linkThreshold1.0_contaMaxDelay-1_addNeg-0.0_unbiased-0.0_l1reg-1_lambdaReg-0.0_globalExtern-0.0_individualExtern-0.0";
	String db = "weibo" ;
	String userCol = "users_2" ;
	
	public void runOriented() {
		
		IC m = new IC(modelfil,2) ;
		m.load();
		
		User.loadUsersFrom(db, userCol) ;
		User.loadAllLinksFrom(db, userCol) ;
		
		double likeIn = 0.0 ;
		double likeOut = 0.0 ;
		double like = 0.0 ;
		int nbIn = 0 ;
		int nbOut = 0 ;
		int zeroes=0 ;
		
		double correct =0.0 ;
		double recall = 0.0 ;
		int nbfound = 0 ;
		double seuil=0.0082;
		
		//double prec = 0.0 ;
		for(String u : User.users.keySet()) {
			HashMap<String, Double> sims ;
			//m.getProba(u, v) ;
			for(String v : User.users.keySet()) {
				if(u.compareTo(v)==0)
					continue ;
				
				double s = m.getProba(u, v)==null ? 0.0 : m.getProba(u, v) ;
				if(s>seuil)
					nbfound++ ;
				if(s==0)
					zeroes++ ;
				s=(0.9998*s+0.0001);
				if(User.users.get(u).getSuccesseurs().containsKey(v) /*|| User.users.get(u).getPredecesseurs().containsKey(v)*/) {
					likeIn = likeIn + s ;
					//System.out.println("in : "+s);
					nbIn ++ ;
					if(s>seuil) {
						correct+=1.0 ;
					}
					like = like + Math.log(s) ;
				} else {
					//likeOut = Math.log((likeOut * (1.0-0.002))+0.001); 
					likeOut = likeOut + s ;
					nbOut++ ;
					like = like + Math.log(1.0-s) ;
					//System.out.println("out : "+ s);
				}
			}
		}
		
		System.out.println("Liens comptes : "+nbIn);
		System.out.println("Liens tho : "+User.users.size() * (User.users.size()-1) * 0.5);
		System.out.println("LikeIn : " + (likeIn / (double)nbIn) ) ;
		System.out.println("LikeOut : " + (likeOut / (double)nbOut) ) ;
		double p =(double)correct/(double)nbfound ;
		double r= (double)correct/(double)nbIn ;
		System.out.println("Precision = "+p);
		System.out.println("Recall = "+r);
		System.out.println("F1 ="+(2*p*r)/(p+r));
		System.out.println("Likelyhood="+like/((double)(nbIn+nbOut)));
		
		double Eprecision = likeIn/(likeIn+likeOut);
		double Erecall = likeIn/((double)nbIn);
		double Ef1 = (2*Eprecision*Erecall)/(Eprecision+Erecall) ;
		
		System.out.println("Epre = "+Eprecision);
		System.out.println("Erec = "+Erecall);
		System.out.println("Ef1 = "+Ef1);
		
	}
	
	public static void main(String args[]) {
		EvalICGraphPred e = new EvalICGraphPred() ;
		e.runOriented() ;
	}
	

}
