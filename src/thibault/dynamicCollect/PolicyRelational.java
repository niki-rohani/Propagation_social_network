package thibault.dynamicCollect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.la4j.vector.Vector;
import org.la4j.vector.Vectors;
import org.la4j.vector.dense.BasicVector;

import thibault.dynamicCollect.UCBVMod.scoreComparator;

public abstract class PolicyRelational {
	
	ArrayList<Arm> arms;
	ArrayList<Arm> lastSelected;
	int nbPlayed=1;
	int sizeFeatures; //features = les coordonnees du mec dans l espace et sizefetaures et la dimension de l espace
	boolean parallel;
	
	public PolicyRelational(int sizeFeatures, boolean parallel) {
		this.arms=new ArrayList<Arm>();
		this.lastSelected=new ArrayList<Arm>();
		this.sizeFeatures=sizeFeatures;
		this.parallel=parallel;
	}
	public void addArm(Arm a){
		arms.add(a);
	}
	
	public void removeArm(Arm a){
		arms.remove(a);
		this.lastSelected=new ArrayList<Arm>();
	}
	
	public void reinitPolicy(){
		this.arms=new ArrayList<Arm>();
	}
	

	public abstract void updateScores();
	public abstract HashSet<Arm> select(int nb);
	public abstract void updateCoordinates();
	public abstract String toString();
	
	
}


 class PolicyRelationalSimple extends PolicyRelational{

	double rate=1;
	public PolicyRelationalSimple(int sizeFeatures, boolean parallel) {
		super(sizeFeatures,parallel);
	}
	
	public PolicyRelationalSimple(int sizeFeatures, boolean parallel,double rate) {
		this(sizeFeatures,parallel);
		this.rate=rate;
	}

	
	@Override
	public void updateScores() {
		
		for(Arm arm:lastSelected){
			arm.sumRewards+=arm.lastReward;
			arm.numberPlayed++;
		}
		if(lastSelected.size()>0){
			if (parallel==true){
				int NTHREADS = Runtime.getRuntime().availableProcessors();
				//System.out.println(NTHREADS);
				ExecutorService exec = Executors.newFixedThreadPool(NTHREADS-1);
				
				final int segmentLen = arms.size() / NTHREADS;
				int offset = 0;
				
				for (int i = 0; i < NTHREADS - 1; i++) {
					final int from = offset;
					final int to = offset + segmentLen;
					exec.execute(new Runnable() {
					@Override
					public void run() {
						updateScore(from, to);
					}});
					offset += segmentLen;
				}
				
				updateScore(arms.size() - segmentLen, arms.size());
				exec.shutdown();
				try {exec.awaitTermination(10, TimeUnit.SECONDS);} 
				catch (InterruptedException ignore) {}
				}
				
				else{
					updateScore(0, arms.size());
				}
		
		nbPlayed++;
		}
	}
	
	@Override
	public void updateCoordinates() {

		if (parallel==true){
		int NTHREADS = Runtime.getRuntime().availableProcessors();
		//System.out.println(NTHREADS);
		ExecutorService exec = Executors.newFixedThreadPool(NTHREADS-1);
		
		final int segmentLen = arms.size() / NTHREADS;
		int offset = 0;
		
		for (int i = 0; i < NTHREADS - 1; i++) {
			final int from = offset;
			final int to = offset + segmentLen;
			exec.execute(new Runnable() {
			@Override
			public void run() {
				updateGrad(from, to);
			}});
			offset += segmentLen;
		}
		
		updateGrad(arms.size() - segmentLen, arms.size());
		exec.shutdown();
		try {exec.awaitTermination(10, TimeUnit.SECONDS);} 
		catch (InterruptedException ignore) {}
		}
		
		else{
			updateGrad(0, arms.size());
		}
		
		for(Arm a:arms){
			//a.features=a.features.subtract(a.grad.multiply(rate));
			System.out.println(a.getName()+"\t"+a.numberPlayed+"\t"+a.sumRewards+"\t"+a.score+"\t"+a.features+"\n");
		}
		
	}
	
	public void updateScore (int from, int to){
		double norm;
		for (int j = from; j < to; j++) {
			Arm a =arms.get(j);
			a.score=0;
			for(Arm aK:arms){
				if(aK!=a){
				norm=a.features.subtract(aK.features).fold(Vectors.mkEuclideanNormAccumulator());
				a.score=a.score+1/Math.pow(norm,2);
				}
			}
		}
	}
	
	public void updateGrad (int from, int to){
		Vector gradJI=new BasicVector(new double[sizeFeatures]);
		Vector gradSum=new BasicVector(new double[sizeFeatures]);
		Vector gradI=new BasicVector(new double[sizeFeatures]);
		Vector gradJ=new BasicVector(new double[sizeFeatures]);
		double norm;
		for (int j = from; j < to; j++) {
			Arm armL=arms.get(j);
			//armL.initGrad();
			
			for(Arm armI:lastSelected){
				for(Arm armJ:lastSelected){
					if(armI.lastReward>armJ.lastReward && 1+armJ.score-armI.score>0){
						for (int i = 0; i<sizeFeatures;i++){
							gradJI.set(i, 0);
							gradI.set(i, 0);
							gradJ.set(i, 0);
							gradSum.set(i, 0);
						}
						
						norm = armL.features.subtract(armI.features).fold(Vectors.mkEuclideanNormAccumulator());
						if(norm!=0){gradI=armL.features.subtract(armI.features).multiply(2/Math.pow(norm, 4));}
						norm = armL.features.subtract(armJ.features).fold(Vectors.mkEuclideanNormAccumulator());
						if(norm!=0){gradJ=armL.features.subtract(armJ.features).multiply(2/Math.pow(norm, 4));}	
						for(Arm armK:arms){
							norm = armL.features.subtract(armK.features).fold(Vectors.mkEuclideanNormAccumulator());
							if(norm!=0){
								gradSum=gradSum.add(armL.features.subtract(armK.features).multiply(2/Math.pow(norm, 4)));
							}
						}
						
						if(armL==armI){
							gradJI=gradSum.subtract(gradI).subtract(gradJ);
						}
						if(armL==armJ){
							gradJI=gradSum.subtract(gradI).subtract(gradJ).multiply(-1);
						}
						else{
							gradJI=gradJ.multiply(-1).add(gradI);
						}	
						//armL.grad=armL.grad.add(gradJI);
					}
				}
			} 

		    }
		}
	
	
	@Override
	public HashSet<Arm> select(int nb) {
		int nbMax=nb;
		if(nbMax>arms.size()){
			nbMax=arms.size();
		}
		lastSelected = new ArrayList<Arm>();

		for(int i=0;i<nbMax;i++){
			lastSelected.add(arms.get(i));
		}
		HashSet<Arm> ret= new HashSet<Arm>(lastSelected);
		return ret;
	}

	@Override
	public String toString(){
		return "simpleRelationalPolicy_nbDim="+sizeFeatures+"_lRate="+rate;
	}


	
}