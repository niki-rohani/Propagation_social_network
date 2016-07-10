package thibault.simulationBandit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;



public class Collect {
	protected int nbArms;
	protected int nbTimeStep;
	protected Policy policy;
	protected int nbToSelect;
	protected int nbSim=2;
	protected int freqReq=10;
	
	public Collect(int nbArms, int nbTimeStep,Policy selectPolicy, int nbToSelect){
		this.nbArms=nbArms;
		this.nbTimeStep=nbTimeStep;
		this.nbToSelect=nbToSelect;
		this.policy=selectPolicy;
	}
	
	public void reinit(){
		policy.reinitPolicy();
	}
	
	public String toString()
	{
		return "nbArms_"+"_"+nbArms+"nbTimeStep"+"_"+nbTimeStep+"Collect_"+"_"+policy+"_nbToSelect="+nbToSelect;
	}
	
	public ArrayList<Double> run(){
		//reinit();
		ArrayList<Double> result =new ArrayList<Double>();

		
		/*for (int i=0;i<nbArms;i++){
			Random mean= new Random();
			Arm a = new Arm(i,mean.nextDouble(),0.1);
			policy.addArm(a);
		}*/
		
		/*if(policy instanceof  UpperBond){
			
			Collections.sort(dataset.arms,new meanComparator());
			double Delta;
			double delta;
			double sum=0.0;
			double meanStar=0.0;
			double meanMin=dataset.arms.get(nbToSelect-1).mean;
			for (int k=0;k<nbToSelect;k++){
				meanStar+=dataset.arms.get(k).mean/nbToSelect;
			}
			
			for (int k=dataset.nbArms-1;k>=nbToSelect;k--){
					Delta=meanStar-dataset.arms.get(k).mean;
					delta=meanMin-dataset.arms.get(k).mean;
					sum+=Delta/(delta*delta);
				}
			
			for (int j=0;j<dataset.nbTimeStep;j++){
				if(j%freqReq==0){
				result.add(sum*8*Math.log(j));
			}
			}

			
		}
		
		else if(policy instanceof  LowerBond){
			
			Collections.sort(dataset.arms,new meanComparator());
			double Delta;
			double KL;
			double sum=0.0;
			double meanMin=dataset.arms.get(nbToSelect-1).mean;
			
			for (int k=dataset.nbArms-1;k>=1;k--){
					Double meanK=dataset.arms.get(k).mean;
					Delta=meanMin-meanK;
					KL=meanK*Math.log(meanK/meanMin)+(1-meanK)*Math.log((1-meanK)/(1-meanMin));
					sum+=Delta/KL;

				}

			
			for (int j=0;j<dataset.nbTimeStep;j++){
				if(j%freqReq==0){
				result.add(sum*Math.log(j*nbToSelect));
			}
			}

			
		}*/
		
		//else{
		for (int j=0;j<nbTimeStep;j++){
			policy.updateRewards();
			HashSet<Arm> selectedArms=policy.select(nbToSelect);
			for(Arm a:selectedArms){
				a.computeLastReward();
				if (policy instanceof ThompsonBeta){
					a.updateFactorsBeta();
					//a.updateFactorsBetaGeneral();
					}
				if (policy instanceof ThompsonGaussian){a.updateFactorsGaussian();;}
				
				
				
			}
			
			if(j%freqReq==0){
					double sumRewards=0.0;
					for (int i=0;i<nbArms;i++){
						sumRewards+=policy.arms.get(i).sumRewards;
					}
					result.add(sumRewards);
				}
			}
		//}
		

		
		/*for(int i=0;i<dataset.nbArms;i++){
			System.out.println("Arm_"+i+"  mean: "+dataset.arms.get(i).mean+"  nbPlayed: "+dataset.arms.get(i).numberPlayed+"  sumRewards: "+dataset.arms.get(i).sumRewards);
		}*/

		 return result;
		 
}
	public class meanComparator implements Comparator<Arm>
	{
		public int compare(Arm arm1,Arm arm2){
			double r1=arm1.mean;
			double r2=arm2.mean;
			if(r1>r2){
				return -1;
			}
			if(r1<r2){
				return 1;
			}
			return 0;
		}
	}

	
	public static void main(String[] args) throws IOException {
	
		
		ArrayList<Collect> collects = new ArrayList<Collect>();
		int nbSimulation = 10;
		int nbArms=1000;
		int nbTimeStep=10000;

		
		//Collect c1=new Collect(100, nbTimeStep,new UCB(),10);
		//Collect c2=new Collect(100, nbTimeStep,new UCBV(),10);
		//Collect c3=new Collect(100, nbTimeStep,new MOSS(),10);
		Collect c4=new Collect(1000, nbTimeStep,new UCB(0.2),10);
		Collect c6=new Collect(1000, nbTimeStep,new RandomP(),10);
		///Collect c5=new Collect(100, nbTimeStep,new ThompsonBeta(),10);
		//Collect c6=new Collect(nbArms, nbTimeStep,new ThompsonGaussian(),10);
		//Collect c7=new Collect(1000, nbTimeStep,new Optimal(),10);
		
		/*Collect c1=new Collect(nbArms, nbTimeStep,new ThompsonBeta(),2);
		Collect c2=new Collect(nbArms, nbTimeStep,new ThompsonBeta(),5);
		Collect c3=new Collect(nbArms, nbTimeStep,new ThompsonBeta(),10);
		Collect c4=new Collect(nbArms, nbTimeStep,new ThompsonBeta(),20);*/



		//collects.add(c1);
		//collects.add(c2);
		//collects.add(c3);
		collects.add(c4);
		//collects.add(c6);
		//collects.add(c5);
		collects.add(c6);



		int nbCol=collects.size();

		ArrayList<ArrayList<ArrayList<Double>>> resultGlob = new ArrayList<ArrayList<ArrayList<Double>>>();
		
		ArrayList<Double> means=new ArrayList<Double>();
		for (int j=0;j<nbArms;j++){
			Random mean= new Random();
			//Arm a = new Arm(i,mean.nextDouble(),0.1);
			means.add(mean.nextDouble());
		}
		
		for (int i=0;i<nbSimulation;i++){
			System.out.println(i);
			
			ArrayList<ArrayList<Double>> resultSim = new ArrayList<ArrayList<Double>>();
			int nbColcur=0;

			for (Collect col : collects){
				col.reinit();
				for (int j=0;j<nbArms;j++){
					//Random mean= new Random();
					Arm a = new Arm(i,means.get(j),0.1);
					col.policy.addArm(a);
				}
				ArrayList<Double> res = col.run();
				resultSim.add(nbColcur,res);
				//System.out.println(res);
				nbColcur++;
			}	
			resultGlob.add(i,resultSim);
		}
		
		int sizeResult=resultGlob.get(0).get(0).size();
		
		

		ArrayList<ArrayList<Double>> finalResult = new ArrayList<ArrayList<Double>>();
		
		for(int i=0;i<nbCol;i++){
			ArrayList<Double> avSim = new ArrayList<Double>();
			for(int k=0;k<sizeResult;k++){
				double av=0.0;
				for(int j=0;j<nbSimulation;j++){
					av+=resultGlob.get(j).get(i).get(k)/nbSimulation;
				}
				avSim.add(k,av);
			}
			finalResult.add(i,avSim);
		}
		
		String fichier = "ResultSim.txt";
		FileWriter fw = new FileWriter(fichier);
		BufferedWriter out = new BufferedWriter(fw);
		//out.write("timeStep"+"\t"+"sumRewards"+"\n");
		//out.write("sumRewards"+"\n");
		
		for(int j=0;j<sizeResult;j++){
			out.write(j*nbTimeStep/sizeResult+"\t");
			for(int i=0;i<nbCol;i++){
				out.write(finalResult.get(i).get(j).toString()+"\t");
			}
			out.write("\n");
		}
		
		/*for (Arm a: d.arms){
			System.out.println(a.mean);
		}*/
		
		
		out.close();

		
	
	}
}