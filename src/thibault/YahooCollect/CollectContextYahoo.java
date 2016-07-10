package thibault.YahooCollect;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.commons.lang3.StringEscapeUtils;



import thibault.SNCollect.*;


public class CollectContextYahoo extends CollectBase{

	protected PolicyCtxtYahoo policyCtxtYahoo;
	protected int sizeFeaturesInd;
	protected int caseContext;
	protected double G=0.0;


	public CollectContextYahoo(Streamer streamer, PolicyBase selectPolicy, int nbArms, long t, PolicyCtxtYahoo policyCtxtYahoo,int sizeFeaturesInd) {
		super(null, null, nbArms, t);
		this.policyCtxtYahoo=policyCtxtYahoo;
		this.sizeFeaturesInd=sizeFeaturesInd;
	}
	
	public CollectContextYahoo(PolicyCtxtYahoo policyCtxtYahoo,int sizeFeaturesInd) {
		super(null, null, 1, 1);
		this.policyCtxtYahoo=policyCtxtYahoo;
		this.sizeFeaturesInd=sizeFeaturesInd;
	}


	public void reinit(){
		//this.armNames=new HashSet<String>();
		policyCtxtYahoo.reinitPolicy();
		//streamer.reinitStreamer();
		this.G=0.0;
	}
	

	public void run(){ 
		reinit();
		this.rewardFunction=null;
		System.out.println("go !");
		boolean ok=true;
		long nbIt=0;

		InputStream ips=null;
		try {
			ips = new FileInputStream("./src/thibault/fileYahoo/ydata-fp-td-clicks-v2_0.20111005");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String line;
		int currentLine=0;


		try {
			while ((line=br.readLine())!=null){
				currentLine++;
				policyCtxtYahoo.possibleArms=new ArrayList<ArmContextYahoo>();
				policyCtxtYahoo.possibleArmsNames=new HashSet<String>();
				
				ArrayList<Integer> nonZeroFeatures = new ArrayList<Integer>();
				
				//System.out.println(line);
				line=line.replace("|", "");
				String[] s1=line.split("user");
				
				/*for(int i=0;i<s1.length;i++){
					System.out.println(s1[i]);
				}*/
				
				String[] s10=s1[0].split(" ");
				
				/*for(int i=0;i<s10.length;i++){
					System.out.println(s10[i]);
				}*/
				
				String displayedArm = s10[1].replace("id-", "");
				
				
				double isClick = Double.parseDouble(s10[2]);
				
				System.out.println(isClick);
				
				//System.out.println(StringEscapeUtils.escapeJava(s1));

				String[] s11=s1[1].split("id-");
				
				/*for(int i=0;i<s11.length;i++){
					System.out.println(s11[i]);
				}*/
				
				String[] s110= s11[0].split(" ");
				
				for(int i=1;i<s11.length;i++){
					policyCtxtYahoo.possibleArmsNames.add(s11[i]);
					if(!policyCtxtYahoo.armsNames.contains(s11[i])){
						policyCtxtYahoo.armsNames.add(s11[i]);
						ArmContextYahoo a=new ArmContextYahoo(s11[i],sizeFeaturesInd);
						policyCtxtYahoo.arms.add(a);
					}
				}
				
				for(int i=1;i<s110.length;i++){
					//System.out.println(s110[i]);
					nonZeroFeatures.add(Integer.parseInt(s110[i])-1);
					
				}
				
				for(int i=0;i<sizeFeaturesInd;i++){
					if(nonZeroFeatures.contains(i)){
						policyCtxtYahoo.featuresVect.setEntry(i, 1.0);
					}
					else{
						policyCtxtYahoo.featuresVect.setEntry(i, 0.0);
					}
					
				}
						
				policyCtxtYahoo.updateScores();

				policyCtxtYahoo.select(nbArms);
				
				//check if it is the same than the one presented in real life
				if(policyCtxtYahoo.lastSelected.getName().equals(displayedArm)){
					policyCtxtYahoo.lastSelected.lastReward=isClick;
					policyCtxtYahoo.updateParameters();
					G+=isClick;
					nbIt++;
					}
				
				System.out.println(policyCtxtYahoo.lastSelected.getName()+" "+displayedArm);
				System.out.println(policyCtxtYahoo.arms.size()+" "+policyCtxtYahoo.possibleArms.size());
				System.out.println("attemp: "+currentLine+"   nbIt: "+nbIt+"   G:"+G);
			}

		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}
}
