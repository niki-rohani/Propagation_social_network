package thibault.simBandit;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;





public class CollectAllVisible {
	public int nbArms;
	public int nbTimeStep;
	public int currentTimeStep=1;
	public Policy policy;
	public int nbToSelect;
	public int freqReq;
	public String simFileName;
	
	public CollectAllVisible(int nbArms, int nbTimeStep,Policy selectPolicy, int nbToSelect, int freqReq,String simFileName){
		this.nbArms=nbArms;
		this.nbTimeStep=nbTimeStep;
		this.nbToSelect=nbToSelect;
		this.policy=selectPolicy;
		this.freqReq=freqReq;
		this.simFileName=simFileName;
	}
	
	public void reinit(){
		this.currentTimeStep=1;
		policy.reinitPolicy();
	}
	
	public String toString(){
		return "nbArms_"+"_"+nbArms+"nbTimeStep"+"_"+nbTimeStep+"CollectAllVisible_"+"_"+policy+"_nbToSelect="+nbToSelect;
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
		try {
			while ((line=br.readLine())!=null){
				String[] st1=line.split(":");
				String[] armLines=st1[1].split(";");
				for(Arm a:policy.arms){
					String armLine = armLines[a.Id];
					a.getContext(armLine);
					if(policy instanceof PlayBest){
						a.seeReward(armLine);
				}
				}

				policy.updateScore();
				policy.select(nbToSelect);
				for(Arm a:policy.lastSelected){
					String armLine = armLines[a.Id];
					a.getReward(armLine);
				}
				policy.updateArmParameter();
				
				if(currentTimeStep%freqReq==0){
					double sumRewards=0.0;
					double sumRewardsStar=0.0;
					for (int i=0;i<nbArms;i++){
						sumRewards+=policy.arms.get(i).sumRewards;
						sumRewardsStar+=policy.arms.get(i).sumRewardsStar;
					}
					resultRwd.add(sumRewards);
					resultRwdStar.add(sumRewardsStar);
					//System.out.println(currentTimeStep+" "+sumRewards);
				}

			currentTimeStep++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		br.close();
		
		if(policy instanceof PlayAll){
			double sumRewards=0.0;
			for(int i=0;i<nbToSelect;i++){
				sumRewards+=policy.arms.get(0).sumRewards;
			}
			//System.out.println(policy.toString()+" "+sumRewards);
			}
		else{
			System.out.println(policy.toString()+" "+resultRwd.get(resultRwd.size()-1));
		}
		
		/*for(int i=0;i<policy.arms.size();i++){
			Arm a =policy.arms.get(i);
			System.out.println(a.Id+" "+a.numberPlayed+" "+a.sumRewards/a.numberPlayed);
		}*/
		result.add(resultRwd);
		result.add(resultRwdStar);
		return result;
	}
}
