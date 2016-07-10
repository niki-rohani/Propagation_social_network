package elie.dataGenerator.execs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import elie.dataGenerator.Representations.*;
import elie.dataGenerator.tools.*;




public class generateTraces {

	/**
	 * @param args
	 * @throws IOException 
	 */

	public static int init(Representation userRep, Item source, ItemSet items){
		int tmp =0;
		while (tmp ==0){
			double[] scores = new double[source.getSuccessors().size()];
			int sum = 0;
			for (int w=0; w <source.getSuccessors().size();w++){
				scores[w] = (double) toolbox.andScore(userRep, items.get(source.getSuccessors().get(w)).getRep());
				sum += scores[w];
			}
			Random rand = new Random();
			float p = rand.nextFloat();
			int w=0;
			double sum2= scores[w]/sum;
			while (sum2< p){	
				w++;
				sum2+= scores[w]/sum;
			}
			tmp =  source.getSuccessors().get(w);
		}
		return tmp;
	}
	public static void main(String[] args) throws IOException {
		int nbItems = 100;
		int nbTracesTrain = 50000;
		int nbTracesTest = 12500;
		double remplissage = 0.1;
		int repSize = 100;
		String file = "/home/guardiae/AMMICO/GreatBlackMusic/Generated/Traces100i50000u";
		String file2 = "/home/guardiae/AMMICO/GreatBlackMusic/Generated/Traces100i50000u";
		ItemSet items = new ItemSet();
		Random rnd = new Random();

		// creation de la source.
		ArrayList<Integer> succp = new ArrayList<Integer>();
		for (int i=0; i<nbItems; i++){
			if (rnd.nextFloat()>=0.1) succp.add(i);
		}
		Item source = new Item(succp, null, 0,null);

		// création du puit.
		Representation rep = new Representation();
		ArrayList<Integer> ef = new ArrayList<Integer>();
		for (int i = 0; i<repSize;i++){
			rep.add(i, rnd.nextBoolean());
			ef.add(0);
		}
		Item puit = new Item(null, rep,1,ef);
		items.add(0,puit);



		// création des items
		for (int j=0; j< nbItems;j++){
			Representation rep2 = new Representation();
			ArrayList<Integer> effect = new ArrayList<Integer>();
			for (int i = 0; i<repSize;i++){
				rep2.add(i, rnd.nextBoolean());
				if (rnd.nextFloat()<0.05){
					effect.add( (int) (2*(rnd.nextInt(2)-0.5)));		
				}else{
					effect.add(0);
				}
			}
			
			ArrayList<Integer> succ = new ArrayList<Integer>();
			for (int i=0; i<nbItems; i++){
				if (rnd.nextFloat()>=0.1) succ.add(i);
			}
			items.add(j+1,new Item(succ, rep2,j+1, effect));
		}


		//création des traces

		String[] traces = new String[nbTracesTrain];
		for (int i = 0; i< nbTracesTrain; i++){
			Representation userRep = new Representation();
			for (int k = 0; k<repSize;k++){
				userRep.add(k, rnd.nextBoolean());
			}


			user usr = new user(new ArrayList<Integer>(), userRep);
			int first = init(userRep, source, items);
			usr.move( first, items.get(first).getEffect());
			int cpt = 0;
			int curritem = usr.getTrace().get(cpt);
			traces[i] =curritem+"";
			
			while(curritem!= 0){
				if (curritem>0)	traces[i]+=":"+curritem;
				cpt++;
				usr.move(curritem, items.get(curritem).getEffect());
				curritem = items.getNext(userRep, curritem);
			}

		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));

		for (int users=0; users<traces.length;users++){
			bf.write(traces[users]+"\n");
		}
		bf.close();
		//création des traces

		traces = new String[nbTracesTrain];
		for (int i = 0; i< nbTracesTrain; i++){
			Representation userRep = new Representation();
			for (int k = 0; k<repSize;k++){
				userRep.add(k, rnd.nextBoolean());
			}


			user usr = new user(new ArrayList<Integer>(), userRep);
			int first = init(userRep, source, items);
			usr.move( first, items.get(first).getEffect());
			int cpt = 0;
			int curritem = usr.getTrace().get(cpt);
			traces[i] =curritem+"";
			while(curritem!= 0){

				if (curritem>0)	traces[i]+=":"+curritem;
				cpt++;
				usr.move(curritem, items.get(curritem).getEffect());
				traces[i]+=":"+curritem;

			}

		}

		 bf = new BufferedWriter(new FileWriter(file2));

		for (int users=0; users<traces.length;users++){
			bf.write(traces[users]+"\n");
		}
		bf.close();
	}

}
