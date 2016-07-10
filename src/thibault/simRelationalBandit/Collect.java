package thibault.simRelationalBandit;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;





public class Collect {
	public int nbArms;
	public int nbTimeStep;
	public int currentTimeStep=1;
	public PolicyRelational policy;
	public int nbToSelect;
	public int freqReq;
	public String simFileName;
	
	public Collect(int nbArms, int nbTimeStep,PolicyRelational selectPolicy, int nbToSelect, int freqReq,String simFileName){
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
	
	public ArrayList<Double> run() throws IOException{
		reinit();
		ArrayList<Double> resultRwd =new ArrayList<Double>();
		InputStream ips=null;
		try {
		ips = new FileInputStream(simFileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String lineCtxt = br.readLine();
		String lineRwd;
		
		try {
			while ((lineRwd=br.readLine())!=null){
				for(ArmRelational a:policy.arms){
					a.getContext(lineCtxt.split(";"));
				}
				policy.updateScore();
				policy.select(nbToSelect);
				for(ArmRelational a:policy.lastSelected){
					a.getReward(lineRwd.split(";"));
				}
				policy.updateArmParameters();
				
				if(currentTimeStep%freqReq==0){
					double sumRewards=0.0;
					for (int i=0;i<nbArms;i++){
						sumRewards+=policy.arms.get(i).sumRewards;
					}
					
					resultRwd.add(sumRewards);
				}
				lineCtxt=lineRwd;
			currentTimeStep++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		br.close();
		return resultRwd;
	}
}
