package thibault.simulationBandit.testBis;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;


public class Dataset {
	
	public ArrayList<Arm> arms;
	public int nbArms;
	public int nbTimeStep;
	public String type;
	//public boolean rand; //true si on choisi les mloyene de facon random, false sin on les met a la main, true par default


	public Dataset(int nbArms, int nbTimeStep, String type) {
		this.nbArms=nbArms;
		this.nbTimeStep=nbTimeStep;
		this.arms=new ArrayList<Arm>();
		this.type=type;
		this.simulateArmsRandomly();
		//this.simulateDistrib();
	}
	
	public Dataset(int nbArms, int nbTimeStep, String type, ArrayList<Double> means) {
		this.nbArms=nbArms;
		this.nbTimeStep=nbTimeStep;
		this.arms=new ArrayList<Arm>();
		this.type=type;
		this.simulateArmsManually(means);
		//this.simulateDistrib();
	}
	
	
	
	public void simulateArmsRandomly(){
		//arm creation
		Arm a;
		Random mean= new Random();
		double var=0.1;
		
		for (int i=0;i<nbArms;i++){
			a=new Arm(i,mean.nextDouble(),var);
			arms.add(i,a);
		}
		
		for (int j=0;j<nbTimeStep;j++){
			for (int i=0;i<nbArms;i++){
				Distributions simValue= new Distributions();
				
				if(type=="bernouilli"){
					boolean val=simValue.nextBoolean(arms.get(i).mean);
					double valbis = val ? 1.0:0.0;
					arms.get(i).rewards.add(j,valbis);
				}
				if(type=="gaussian"){
					double val=simValue.nextGaussian(arms.get(i).mean, arms.get(i).var);
					arms.get(i).rewards.add(j,val);
				}
			}
		}

		}
	
	public void simulateArmsManually(ArrayList<Double> means){
		//arm creation
		Arm a;
		
		double var=0.01;
		for (int i=0;i<nbArms;i++){
			a=new Arm(i,means.get(i),var);
			arms.add(i,a);
		}
		
		for (int j=0;j<nbTimeStep;j++){
			for (int i=0;i<nbArms;i++){
				Distributions simValue= new Distributions();
				
				if(type=="bernouilli"){
					boolean val=simValue.nextBoolean(arms.get(i).mean);
					double valbis = val ? 1.0:0.0;
					arms.get(i).rewards.add(j,valbis);
				}
				if(type=="gaussian"){
					double val=simValue.nextGaussian(arms.get(i).mean, arms.get(i).var);
					arms.get(i).rewards.add(j,val);
				}
			}
		}

		}
	
	
	
		//simulation
	public void simulateDistrib(){
		for (int j=0;j<nbTimeStep;j++){
			for (int i=0;i<nbArms;i++){
				Distributions simValue= new Distributions();
				if(type=="bernouilli"){
				boolean val=simValue.nextBoolean(arms.get(i).mean);
				double valbis = val ? 1.0:0.0;
				arms.get(i).rewards.set(j,valbis);
				}
				
				if(type=="gaussian"){
				double val=simValue.nextGaussian(arms.get(i).mean, arms.get(i).var);
				arms.get(i).rewards.set(j,val);
				}
				
			}
		}
	}

	public void writeSimulation() throws IOException{
		 String fichier = "globSim.txt";
		 FileWriter fw = new FileWriter(fichier);
		 BufferedWriter out = new BufferedWriter(fw);
		 
		 out.write("timeStep"+"\t");
		 
		 for (int i=0;i<nbArms;i++){
			 out.write("Arm"+" "+i+"\t");
		 }
		 
		 out.write("\n");
		 
		 for (int j=0;j<nbTimeStep;j++){
			 out.write(j+"\t");
			 for (int i=0;i<nbArms;i++){
				 out.write(arms.get(i).rewards.get(j)+"\t");
			 }
			 out.write("\n");
		 }
		 out.close();
	}
	
	public void reinit(){
		for (int i=0;i<nbArms;i++){
			arms.get(i).reinit();
		}
	}
	
	/*public static void main(String[] args) throws IOException {
		Dataset d=new Dataset(8000,500);
		d.writeSimulation();
	}*/

}
