package thibault.simBandit;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

public class CollectHiddenRandObs extends CollectAllVisible{

	public int nbObserved;
	public CollectHiddenRandObs(int nbArms, int nbTimeStep, Policy selectPolicy, int nbToSelect, int freqReq, String simFileName) {
		super(nbArms, nbTimeStep, selectPolicy, nbToSelect, freqReq, simFileName);
		
	}
	
	public String toString(){
		return "nbArms_"+"_"+nbArms+"nbTimeStep"+"_"+nbTimeStep+"CollectHidden_"+"_"+policy+"_nbToSelect="+nbToSelect;
	}

	public ArrayList<ArrayList<Double>> run() throws IOException{
		reinit();
		ArrayList<ArrayList<Double>> result=new ArrayList<ArrayList<Double>>();
		ArrayList<Double> resultRwd =new ArrayList<Double>();
		ArrayList<Double> resultRwdStar =new ArrayList<Double>();
		InputStream ips=null;
		try {
		ips = new FileInputStream(simFileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String line;
		
		Random rand = new Random();
		Collections.shuffle(policy.arms);
		nbObserved= rand.nextInt((policy.arms.size() - 1) + 1) + 1;
		for(int i=0;i<nbObserved;i++){
			policy.observedArms.add(policy.arms.get(i));
		}
		try {
			while ((line=br.readLine())!=null){
				String[] st1=line.split(":");
				String[] armLines=st1[1].split(";");
				for(Arm a:policy.observedArms){
					//System.out.println(a.Id +" "+a.numberObserved+" "+a.score);
						String armLine = armLines[a.Id];
						a.getContext(armLine);
				}
				//System.out.println(currentTimeStep);
				//System.out.println();
				policy.updateScore();
				policy.select(nbToSelect);
				for(Arm a:policy.lastSelected){
					String armLine = armLines[a.Id];
					a.getReward(armLine);
				}
				policy.updateArmParameter();

				nbObserved= rand.nextInt((policy.arms.size() - 1) + 1) + 1;
				Collections.shuffle(policy.arms);
				policy.observedArms=new HashSet<Arm>();
				for(int i=0;i<nbObserved;i++){
					policy.observedArms.add(policy.arms.get(i));
				}
				//policy.observedArms=new HashSet<Arm>(policy.lastSelected);
				
				if(currentTimeStep%freqReq==0){
					double sumRewards=0.0;
					for (int i=0;i<nbArms;i++){
						sumRewards+=policy.arms.get(i).sumRewards;
					}
					resultRwd.add(sumRewards);
					//resultRwdStar.add(sumRewards);
					//System.out.println(currentTimeStep+" "+sumRewards);
				}
			currentTimeStep++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		br.close();
		/*for(int i=0;i<policy.arms.size();i++){
			Arm a =policy.arms.get(i);
			System.out.println(a.Id+" "+a.numberPlayed+" "+a.sumRewards/a.numberPlayed);
		}*/
		System.out.println(policy.toString()+" "+resultRwd.get(resultRwd.size()-1));
		result.add(resultRwd);
		return result;
	}
	
}
