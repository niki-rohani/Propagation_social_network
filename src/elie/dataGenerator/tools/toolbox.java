package elie.dataGenerator.tools;

import java.util.ArrayList;
import java.util.Random;

import elie.dataGenerator.Representations.*;

public class toolbox {
	public static int andScore(Representation user, Representation item){
		int sum = 0;
		for (int i=0; i<user.size(); i++){
			if (user.get(i)&&item.get(i)){
				sum++;
			}
		}
		
		return sum;
	}
	
	public static int pickOne(Representation userRep, ArrayList<Integer> successors,ItemSet items){
		double[] scores = new double[successors.size()];
		int sum = 0;
		for (int i=0; i <successors.size();i++){
			scores[i] = (double) andScore(userRep, items.get(successors.get(i)).getRep());
			sum += scores[i];
		}
		
		Random rand = new Random();
		float p = rand.nextFloat();
		int i=0;
		double sum2= scores[i]/sum;
		while (sum2< p){	
			i++;
			sum2+= scores[i]/sum;
		}
		return successors.get(i);
	}

	public static Representation updateRep(Representation rep, ArrayList<Integer> filtre) {
		for (int i=0;i<rep.size();i++){
			int j = filtre.get(i);
			if(j>0){
				rep.set(i, true);
			}else if (j<0){
				rep.set(i, false);	
			}
		}
		return rep;
	}
}
