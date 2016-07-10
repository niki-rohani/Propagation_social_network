package thibault.simulationBandit.testBis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;



public class Collect {
	protected Policy policy;
	protected int nbToSelect;
	protected int nbSim=2;
	protected int freqReq=100;
	
	public Collect(Policy selectPolicy, int nbToSelect){
		this.nbToSelect=nbToSelect;
		this.policy=selectPolicy;
	}
	
	public void reinit(){
		policy.reinitPolicy();
	}
	
	public String toString()
	{
		return "Collect_"+"_"+policy+"_nbToSelect="+nbToSelect;
	}
	
	public ArrayList<Double> run(Dataset dataset) throws IOException{ 
		reinit();
		ArrayList<Double> result =new ArrayList<Double>();
		//String fichier = "result_"+policy.toString()+"_nbArms_"+dataset.nbArms+"_toSelect_"+nbToSelect+".txt";
		//FileWriter fw = new FileWriter(fichier);
		//BufferedWriter out = new BufferedWriter(fw);
		//out.write("timeStep"+"\t"+"sumRewards"+"\n");
		//out.write("sumRewards"+"\n");
		
		for (int i=0;i<dataset.nbArms;i++){
			policy.addArm(dataset.arms.get(i));
		}
		
		if(policy instanceof  UpperBond){
			
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

			
		}
		
		else{
		for (int j=0;j<dataset.nbTimeStep;j++){
			policy.updateRewards();
			HashSet<Arm> selectedArms=policy.select(nbToSelect);
			for(Arm a:selectedArms){
				a.computeLastReward(j);
				a.updateFactorsBeta();
			}
			
			if(j%freqReq==0){
					double sumRewards=0.0;
					for (int i=0;i<dataset.nbArms;i++){
						sumRewards+=dataset.arms.get(i).sumRewards;
					}
					//sumRewards=sumRewards/nbToSelect;
					//out.write(j+"\t"+sumRewards+"\n");
					//out.write(sumRewards+"\n");
					result.add(sumRewards);
				}
			}
		}
		
		 //out.close();
		
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
	
		
		int nbArms = 100;
		int nbTimeStep = 40000;
		int nbSimulation = 10;
		ArrayList<Double> means=new ArrayList<Double>();
		for(int i =1;i<=nbArms+1;i++){
			means.add(1.0*(i-0.5)/nbArms);
		}
		Dataset d=new Dataset(nbArms,nbTimeStep,"bernouilli");
		ArrayList<Collect> collects = new ArrayList<Collect>();
		
		/*d.arms.get(0).mean=0.9;
		d.arms.get(1).mean=0.9;
		d.arms.get(2).mean=0.9;
		d.arms.get(3).mean=0.1;
		d.arms.get(4).mean=0.1;
		d.arms.get(5).mean=0.1;
		d.arms.get(6).mean=0.1;
		d.arms.get(7).mean=0.1;
		d.arms.get(8).mean=0.1;
		d.arms.get(9).mean=0.1;*/
		
		/*Collect c1=new Collect(new UCB(),1);
		Collect c2=new Collect(new UCB(),2);
		Collect c3=new Collect(new UCB(),3);
		Collect c4=new Collect(new UCB(),4);
		Collect c5=new Collect(new UCB(),5);
		Collect c6=new Collect(new UCB(),6);
		Collect c7=new Collect(new UCB(),7);
		Collect c8=new Collect(new UCB(),8);
		Collect c9=new Collect(new UCB(),9);
		Collect c10=new Collect(new UCB(),10);*/
		
		Collect c1=new Collect(new UCB(),10);
		Collect c2=new Collect(new UCBV(),10);
		Collect c3=new Collect(new MOSS(),10);
		Collect c4=new Collect(new UCB(0.2),10);
		Collect c5=new Collect(new ThompsonBeta(),10);
		Collect c6=new Collect(new Optimal(),10);
		//Collect c7=new Collect(new UCBV(),3);
		/*Collect c6=new Collect(new UCBV(),6);
		Collect c7=new Collect(new UCBV(),7);
		Collect c8=new Collect(new UCBV(),8);
		Collect c9=new Collect(new UCBV(),9);
		Collect c10=new Collect(new UCBV(),10);*/


		//Collect c3=new Collect(new UCBV(),1);
		//Collect c3=new Collect(new UCB(),5);
		//Collect c4=new Collect(new UCB(),10);
		//Collect c4=new Collect(new UCBV(),5);
		//Collect collect2=new Collect(new Random(),1);
		//Collect collect5=new Collect(new UCBV(),1);
		
		collects.add(c1);
		collects.add(c2);
		collects.add(c3);
		collects.add(c4);
		collects.add(c5);
		collects.add(c6);
		/*collects.add(c7);
		collects.add(c8);
		collects.add(c9);
		collects.add(c10);*/

		int nbCol=collects.size();

		ArrayList<ArrayList<ArrayList<Double>>> resultGlob = new ArrayList<ArrayList<ArrayList<Double>>>();
		
		for (int i=0;i<nbSimulation;i++){
			System.out.println(i);
			ArrayList<ArrayList<Double>> resultSim = new ArrayList<ArrayList<Double>>();
			d.simulateDistrib();
			int nbColcur=0;
			for (int j=0;j<collects.size();j++){
				Collect col = collects.get(j);
			
			//for (Collect col : collects){
				d.reinit();
				ArrayList<Double> res = col.run(d);
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
			out.write(j*d.nbTimeStep/sizeResult+"\t");
			for(int i=0;i<nbCol;i++){
				out.write(finalResult.get(i).get(j).toString()+"\t");
			}
			out.write("\n");
		}
		
		for (Arm a: d.arms){
			System.out.println(a.mean);
		}
		
		
		out.close();

		
	
	}
}