package thibault.simRelationalBandit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import thibault.simBandit.Arm;



public class LaunchCollect {

	public static void main(String[] args) throws IOException {
		int nbArms=6;
		int nbTimeStep=1000;
		int nbToSelect=1;
		int freqReq=10;
		int n=100;
		int sizeSpace=2;
		
		ArrayList<PolicyRelational> policies = new ArrayList<PolicyRelational>(); 
		//policies.add(new RandomPolicy());
		//policies.add(new UCB());
		policies.add(new LinUCB());
		policies.add(new LinUCBGrad(nbArms,n));
		//policies.add(new EpsilonGreedyEmbeddings(sizeSpace,n));
		//policies.add(new EpsilonGreedyEmbeddings2(sizeSpace,n));
		
		String  format = "dd.MM.yyyy_H.mm.ss";
		java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
		Date date = new Date(); 
		String sdate=formater.format(date);

		String outputRep="simRelational_"+sdate;

		File rep=new File(outputRep);

		if(!rep.exists()){
			rep.mkdirs();
		}
		
		DataGeneratorRelationalBandit2 b=new DataGeneratorRelationalBandit2(nbArms);
		b.genere(outputRep, nbTimeStep);
		String simFileName=outputRep+"/"+"sim.txt";
		for(PolicyRelational p: policies){
			for(int i=0;i<nbArms;i++){
				ArmRelational a=new ArmRelational(i,nbArms,sizeSpace);
				p.arms.add(a);
			}
			Collect c = new Collect(nbArms,  nbTimeStep, p,  nbToSelect,  freqReq, simFileName);
			ArrayList<Double> res = c.run();
			System.out.println(p.toString()+" "+res.get(res.size()-1));
			for(ArmRelational a:p.arms){
				System.out.println(a.Id+" "+a.thetaVec);
				//System.out.println(a.Id+" "+a.coords);
				//System.out.println(a.Id+" "+a.numberPlayed);
			}
		}
	}

}
