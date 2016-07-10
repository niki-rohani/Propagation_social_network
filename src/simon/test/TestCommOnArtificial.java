package simon.test;

import java.util.HashMap;

import propagationModels.PropagationStruct;
import simon.tools.CascadesComGen;

public class TestCommOnArtificial {
	
	public static void main(String args[]) {
		
		int nbusers = Integer.parseInt(args[0]);
		int nbCom =Integer.parseInt(args[1]);
		double probAct = Double.parseDouble(args[2]);
		double probPass= Double.parseDouble(args[3]);
		double noize= Double.parseDouble(args[4]);
		int maxSources= Integer.parseInt(args[5]);
		double probaStop= Double.parseDouble(args[5]);
		int nbTrain= Integer.parseInt(args[6]);
		int nbTest= Integer.parseInt(args[7]);
		
		nbusers = 100 ;
		nbCom = 8;
		probAct = 0.05;
		probPass= 0.1;
		noize= 0.1;
		maxSources= 3;
		probaStop= 0.2;
		nbTrain = 4000 ;
		nbTest = 1000 ;
		
		CascadesComGen gen = new CascadesComGen(nbusers,nbCom,probAct,probPass,noize,maxSources,probaStop) ;
		
		HashMap<Integer, PropagationStruct> train = gen.generate(nbTrain) ;
		
	}
	
}
