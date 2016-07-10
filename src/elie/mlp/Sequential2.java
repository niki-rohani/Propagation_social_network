package elie.mlp;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import core.Structure;
import core.User;
import propagationModels.MLP;
import mlp.* ;

public class Sequential2 extends MLPModel {

	private HashMap<Integer,CPUParams> items_z ;
	private HashMap<Integer,CPUParams> items_z_out ;
	private ArrayList<Trace> training_traces ;
	private ArrayList<Double> pops ;
	private HashMap<Integer,Integer> rooms ;
	
	private TableModule zprecAndZtrue = new TableModule() ;
	private TableModule zprecAndZfalse = new TableModule() ;
	private TableModule zprecAndZprec = new TableModule() ;
	
	private SequentialModule previousZsum = new SequentialModule();
	
	
	private Module outputForInference ;

	private int nbItems;

	private int dim;
	private int batchsize;
	private double lambda;
	private double mu;
	
	
	public Sequential2(int nbItems, int dim, String trainingfile, int batchsize, String modelfile, double lambda, double mu) throws IOException {
		
		this.nbItems=nbItems ;
		this.dim = dim ;
		this.params=new Parameters();
		this.batchsize = batchsize ;
		this.model_file = modelfile ;
		this.lambda=lambda ;
		this.mu=mu ;
		
		CPUParams zprec = new CPUParams(batchsize, dim)  ;
		CPUParams ztrue = new CPUParams(batchsize, dim)  ;
		CPUParams zfalse = new CPUParams(batchsize, dim)  ;
		
		
		System.out.println("Loading...");
		this.loadFile(trainingfile);
		this.computePops(); 
		//this.loadRooms(roomsFile) ;
		System.out.println("Loading complete");
		
		this.init_z(nbItems,dim) ;
		
		zprecAndZtrue = new TableModule() ;
		zprecAndZtrue.name = "zprecAndZtrue" ;
		zprecAndZtrue.addModule(zprec);
		zprecAndZtrue.addModule(ztrue);
		
		ArrayList<Double> w = new ArrayList<Double>() ;
		w.add(1.0);
		w.add(-1.0) ;
		CPUSum zprecMinusZtrue = new CPUSum(dim,2,w) ;
		zprecMinusZtrue.name = "zprecMinusZtrue" ;
		
		CPUL2Norm distPrecTrue = new CPUL2Norm(dim) ;
		distPrecTrue.name = "distPrecTrue" ;
		
		SequentialModule dist1 = new SequentialModule() ;
		dist1.name = "dist1" ;
		dist1.addModule(zprecAndZtrue);
		dist1.addModule(zprecMinusZtrue);
		dist1.addModule(distPrecTrue);
		
		this.outputForInference = dist1 ;
		
		zprecAndZfalse = new TableModule() ;
		zprecAndZfalse.name = "zprecAndZfalse" ;
		zprecAndZfalse.addModule(zprec);
		zprecAndZfalse.addModule(zfalse);
		
		CPUSum zprecMinusZfalse = new CPUSum(dim,2,w) ;
		zprecMinusZfalse.name = "zprecMinusZfalse" ;
		
		CPUL2Norm distPrecFalse = new CPUL2Norm(dim) ;
		distPrecFalse.name = "distPrecFalse" ;
		
		/*TableModule allDists = new TableModule() ;
		allDists.setModule(0, distPrecFalse);
		allDists.setModule(0, distPrecTrue);*/
		
		SequentialModule dist2 = new SequentialModule() ;
		dist2.name = "dist2" ;
		dist2.addModule(zprecAndZfalse);
		dist2.addModule(zprecMinusZfalse);
		dist2.addModule(distPrecFalse);
		
		TableModule bothDists = new TableModule();
		bothDists.name = "bothDists" ;
		bothDists.addModule(dist2);
		bothDists.addModule(dist1);
		
		CPUSum distDiff = new CPUSum(1, 2, w) ;
		distDiff.name = "distDiff" ;
		
		SequentialModule seq = new SequentialModule() ;
		seq.addModule(bothDists);
		seq.addModule(distDiff); 
		
		Criterion cri = new CPUHingeLoss(1) ;
		seq.addModule(cri);
		
		// Regularisation 1 (dist prec-prec)
		zprecAndZprec = new TableModule() ;
		zprecAndZprec.name = "zprecAndZprec" ;
		zprecAndZprec.addModule(zprec);
		zprecAndZprec.addModule(zprec);
		
		CPUSum zprecMinusZprec = new CPUSum(dim,2,w) ;
		zprecMinusZprec.name = "zprecMinusZprec" ;
		
		CPUL2Norm distPrecPrec = new CPUL2Norm(dim) ;
		distPrecPrec.name = "distPrecPrec" ;
		
		SequentialModule dist3 = new SequentialModule() ;
		dist3.name = "dist3" ;
		dist3.addModule(zprecAndZprec);
		dist3.addModule(zprecMinusZprec);
		dist3.addModule(distPrecPrec);
		
		// Regulation 2 (norm L2)
		CPUParams par=new CPUParams(1,params.size());
	 	params.giveAllParamsTo(par);
	 	CPUL2Norm norm=new CPUL2Norm(par.getNbParams());
	 	SequentialModule l2Regu = new SequentialModule() ;
	 	l2Regu.name = "l2Regu" ;
	 	l2Regu.addModule(par);
	 	l2Regu.addModule(norm);
	 	l2Regu.addModule(new CPUTimesVals(1, 0.5/((double)nbItems)));
		
		TableModule LossAndRegu = new TableModule() ;
		LossAndRegu.name = "LossAndRegu" ;
		LossAndRegu.addModule(seq);
		LossAndRegu.addModule(dist3);
		
		ArrayList<Double> w2 = new ArrayList<Double>() ;
		w2.add(1.0);
		w2.add(lambda) ;
		CPUSum LossPlusRegu = new CPUSum(1, 2,w2) ;
		LossPlusRegu.name = "LossPlusRegu" ;
		
		
		SequentialModule avgLossPlusRegu = new SequentialModule() ;
		avgLossPlusRegu.name = "avgLossPlusRegu" ;
		avgLossPlusRegu.addModule(LossAndRegu);
		avgLossPlusRegu.addModule(LossPlusRegu);
		avgLossPlusRegu.addModule(new CPUAverageRows());
		
		TableModule all = new TableModule() ;
		all.addModule(avgLossPlusRegu);
		all.addModule(l2Regu);
		ArrayList<Double> w3 = new ArrayList<Double>() ;
		w3.add(1.0);
		w3.add(mu) ;
		CPUSum all2 = new CPUSum(1, 2,w3) ;
		
		global = new SequentialModule();
		global.addModule(all);
		global.addModule(all2);
		
		
	}
	
	private void loadRooms(String roomsFile) throws IOException {
		
		this.rooms = new HashMap<Integer,Integer>() ;
		BufferedReader bf = new BufferedReader(new FileReader(roomsFile));
		String line = bf.readLine();
		int j= 0; 
		while(line != null){
			String[] parts= line.split(":");
			rooms.put(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])) ;
			line = bf.readLine();
		}
		bf.close();
		
	}

	private void computePops() {
		pops = new ArrayList<Double>() ;
		for(int i=0 ; i<nbItems ; i++) {
			pops.add(0.0) ;
		}
		for(Trace t : training_traces) {
			 for(int i : t) {
				 pops.set(i,pops.get(i)+1.0) ;
			 }
		}
	}
	
	private void loadFile(String file) throws IOException {
		BufferedReader bf = new BufferedReader(new FileReader(file));
		String line = bf.readLine();
		training_traces = new ArrayList<Trace>();
		int j= 0; 
		while(line != null){
			String[] parts= line.split(":");
			Trace trace = new Trace();
			for(int i=0;i<parts.length;i++){
				trace.add(Integer.parseInt(parts[i]));
			}
			if(trace.size()>1) 
				training_traces.add(trace);
			System.out.println("Trace "+j+" loaded.");
			j++ ;
			line = bf.readLine();
		}
		bf.close();
	}
	
	private void init_z(int nbItems,int dim) {
		items_z=new HashMap<Integer,CPUParams>() ;
		items_z_out=new HashMap<Integer,CPUParams>() ;
		for(int i=0 ; i<nbItems ; i++) {
			CPUParams p = new CPUParams(1, dim) ;
			this.items_z.put(i,p) ;
			this.params.allocateNewParamsFor(p, -1.0, 1.0);
			CPUParams p2 = new CPUParams(1, dim) ;
			this.items_z_out.put(i, p2) ;
			this.params.allocateNewParamsFor(p2, -1.0, 1.0);
		}
	}
	
	public int infer(Structure struct) {
		
		
		return 0;
	}
	
	public Tensor inferOne(ArrayList<Integer> previousItems) {

		/*CPUParams zprec = new CPUParams(nbItems, dim)  ;
		CPUParams ztrue = new CPUParams(nbItems, dim)  ;
		CPUParams zfalse = new CPUParams(nbItems, dim)  ;0.00
		
		for(int i=0 ; i<nbItems ; i++) {
			CPUParams pprec = this.items_z.get(previousItem) ;
			CPUParams ptrue = this.items_z.get(i) ;
			CPUParams pfalse= this.items_z.get(i) ;
		}
		
		this.zprecAndZfalse.setModule(0,zprec);
		this.zprecAndZfalse.setModule(1,zfalse);
		this.zprecAndZtrue.setModule(0,zprec);
		this.zprecAndZtrue.setModule(1,ztrue);
		
		global.forward(null);*/
		
		int previousItem = previousItems.get(previousItems.size()-1) ;
		
		Matrix m = new CPUMatrix(nbItems,1) ;
		for(int i = 0 ; i<nbItems ; i++) {
			double dist = 0.0 ;
			for(int compo = 0 ; compo<dim ; compo++) {
				dist += Math.pow(items_z.get(i).getParameters().getMatrix(0).getValue(0, compo) - items_z_out.get(previousItem).getParameters().getMatrix(0).getValue(0, compo),2) ;
			}
			dist = Math.sqrt(dist) ;
			m.setValue(i, 0, dist);
		}
		for(int prev : previousItems)
			m.setValue(prev, 0, Double.POSITIVE_INFINITY);
		
		//return outputForInference.getOutput() ;
		return new Tensor(m) ;
		

	}
	
	public Tensor inferOneBaseline(ArrayList<Integer> previousItems) {
		int previousItem = previousItems.get(previousItems.size()-1) ;
		int previousRoom = rooms.get(previousItem) ;
		Matrix m = new CPUMatrix(nbItems,1) ;
		for(int i = 0 ; i<nbItems ; i++) {
			if(previousItems.indexOf(i) == -1 && rooms.get(i)==previousRoom) {
				m.setValue(i, 0, 1);
			} else {
				m.setValue(i, 0, 10);
			}
		}
		return new Tensor(m) ;
	}

	@Override
	public void load() throws IOException {
		File file=new File(model_file);
    	BufferedReader f = new BufferedReader(new FileReader(file)) ;
    	
    	
    	f.readLine() ;
    	this.nbItems=Integer.parseInt(f.readLine().substring(7)) ;
    	this.dim=Integer.parseInt(f.readLine().substring(4)) ;
    	
    	f.readLine() ;

 		int nbpar = 0 ;
 		ArrayList<Parameter> thisparams = this.params.getParams() ;
 		for(String s = f.readLine() ; s.compareTo("</PARAMETERS>")!=0 ; s=f.readLine()) {
    		thisparams.get(nbpar).setVal(Double.parseDouble(s)) ;
    		nbpar++ ;
    	}
 		//if(nbpar != (nbCom*nbUsers+nbCom*nbCom+nbUsers*nbCom)) {
 		if(nbpar != (dim*nbItems)) {
 			throw new IOException("Wrong number of parameters in file"+model_file+". Expected "+ dim*nbItems+", found "+nbpar) ;
 		}

	}

	@Override
	public void save() throws IOException {
		//throw new RuntimeException("Being implemented") ;
				File file=new File(model_file);
		    	File dir = file.getParentFile();
		    	if(dir!=null){
		    		dir.mkdirs();
		    	}
		    	
		    	PrintStream p = new PrintStream(file) ;
		    	p.println("<MODEL="+this+">");
		    	p.println("nbItem="+this.nbItems);
		    	p.println("dim="+this.dim) ;
		    	
		    	p.println("<PARAMETERS>") ;
		    	int nbp=0 ;
		    	for(Parameter par :this.params.getParams()) {
		    		p.println(par.getVal()) ;
		    		nbp++ ;
		    	}
		    	System.out.println("PARAM SAVED : "+nbp );
		    	
		    	
		    	
		    	p.println("</PARAMETERS>") ;
		    	
				p.println("</MODEL>") ;

	}

	@Override
	public void forward() {
			
			/*zprec = new CPUParams(batchsize,dim) ;
			ztrue = new CPUParams(batchsize,dim) ;
			zfalse = new CPUParams(batchsize,dim) ;
		     */
			
	
		
		CPUParams zprecout = new CPUParams(batchsize, dim)  ;
		CPUParams zprecin = new CPUParams(batchsize, dim)  ;
		CPUParams ztrue = new CPUParams(batchsize, dim)  ;
		CPUParams zfalse = new CPUParams(batchsize, dim)  ;
		
		
			Random rng = new Random() ;
			
			
			for(int i=0 ; i<batchsize ; i++) {
				
				int id_trace = rng.nextInt(this.training_traces.size()) ;
				Trace t = this.training_traces.get(id_trace) ;
				int rank_item_prec = rng.nextInt(t.size()-1) ;
				int id_item_prec = t.get(rank_item_prec) ;
				
				int windowsNextItem = t.size() -1 - rank_item_prec ;
				int rankNextItem = rank_item_prec + rng.nextInt(windowsNextItem) +1 ;
				int id_item_true = t.get(rankNextItem) ;
				
				/*int offsetNextItemAgain = t.size() - (rank_item_prec+off) ;
				int offAgain = rng.nextInt(offsetNextItemAgain) ;*/
				
				/*int id_item_true = t.get(rank_item_prec+off) ; */
				int id_item_false = id_item_true ;
				while( t.indexOf(id_item_false) != -1 && t.indexOf(id_item_false)<=rankNextItem )
					id_item_false = rng.nextInt(this.nbItems) ;
				
				
				//System.out.println(t + "-> " + id_item_prec + ","+id_item_true+","+id_item_false);
				
				//CPUParams pprec = this.items_z.get(id_item_prec) ;
				CPUParams pprecout = this.items_z_out.get(id_item_prec) ;
				CPUParams pprecin = this.items_z.get(id_item_prec) ;
				CPUParams ptrue = this.items_z.get(id_item_true) ;
				CPUParams pfalse= this.items_z.get(id_item_false) ;
				
				zprecout.addParametersFrom(pprecout);
				zprecin.addParametersFrom(pprecin);
				ztrue.addParametersFrom(ptrue);
				zfalse.addParametersFrom(pfalse);
				
			}
			
			this.zprecAndZfalse.setModule(0,zprecout);
			this.zprecAndZfalse.setModule(1,zfalse);
			this.zprecAndZtrue.setModule(0,zprecout);
			this.zprecAndZtrue.setModule(1,ztrue);
			this.zprecAndZprec.setModule(0,zprecout);
			this.zprecAndZprec.setModule(1,zprecin);
			
			global.forward(null);

	}
	
	public void learn(int nbIterMax,double initLine, double stepLine) {
		
		LineSearch lsearch=LineSearch.getFactorLine(initLine,stepLine); 
 		DescentDirection dir=DescentDirection.getGradientDirection();
 		Optimizer optim = Optimizer.getDescent(dir, lsearch) ;
 		optim.optimize(this,0.00001,nbIterMax,false) ;
	}
	
	public void eval(String testfile ) throws IOException {
		
		ArrayList<Trace> testing_traces = new ArrayList<Trace>() ;
		
		BufferedReader bf = new BufferedReader(new FileReader(testfile));
		String line = bf.readLine();
		int j= 0; 
		while(line != null){
			String[] parts= line.split(":");
			Trace trace = new Trace();
			for(int i=0;i<parts.length;i++){
				trace.add(Integer.parseInt(parts[i]));
			}
			testing_traces.add(trace);
			System.out.println("Trace "+j+" loaded.");
			j++ ;
			line = bf.readLine();
		}
		bf.close();
		
		
		/*Trace t2 = new Trace() ;
		t2.add(0) ;
		t2.add(1) ;
		t2.add(2) ;
		t2.add(3) ;
		t2.add(4) ;
		testing_traces=new ArrayList<Trace>() ;
		testing_traces.add(t2) ;*/
		
		
		// MEANRANK
		double meanrank = 0 ;
		int cpt = 0 ;
		// Ensuite, le test.
		for(Trace t : testing_traces) {
			ArrayList<Integer> previousItems = new ArrayList<Integer>() ;
			for(int i=0 ; i < t.size()-1 ; i++) {
				int prevItem = t.get(i) ;
				previousItems.add(prevItem) ;
				int nextItem = t.get(i+1) ;
				Tensor scores =this.inferOne(previousItems) ;
				//Tensor base =this.inferOneBaseline(previousItems) ;
				Matrix finalscores = new CPUMatrix(nbItems, 1) ;
				for(int j1=0 ; j1<nbItems ;j1++) {
					finalscores.setValue(j1, 0, scores.getMatrix(0).getValue(j1, 0)/*+base.getMatrix(0).getValue(j1, 0)*/) ;
				}
				int rank = getRank(new Tensor(finalscores),nextItem) ;
				//int rank = rank2+rankbase /2 ;
				//System.out.print(rank+ " ");
				meanrank += (double)rank ;
				cpt++ ;
			}
			//System.out.println();
			
		}
		meanrank /= cpt ;
		System.out.println("Mean rank = "+meanrank);
		
		// SOFTMAX
//		double meansoftmax = 0 ;
//		cpt = 0 ;
//		// Ensuite, le test.
//		for(Trace t : testing_traces) {
//			ArrayList<Integer> previousItems = new ArrayList<Integer>() ;
//			for(int i=0 ; i < t.size()-1 ; i++) {
//				int prevItem = t.get(i) ;
//				previousItems.add(prevItem) ;
//				int nextItem = t.get(i+1) ;
//				Tensor scores =this.inferOne(previousItems) ;
//				double soft = getSoftMax(scores,nextItem) ;
//				meansoftmax += soft ;
//				cpt++ ;
//			}
//			
//		}
//		meansoftmax /= cpt ;
//		System.out.println("Mean softmin = "+meansoftmax);
	}
	
	private double getSoftMax(Tensor scores, int nextItem) { // getSoftMIN, en fait....
		
		Matrix m = scores.getMatrix(0) ;
		double sumExp=0.0 ;
		for(int j = 0 ; j<m.getNumberOfRows() ; j++) {
			sumExp += Math.exp(m.getValue(j, 0)) ;
		}
		return Math.exp(m.getValue(nextItem, 0)) / sumExp ;
		
	}

	private int getRank(Tensor scores, int i) {
		Matrix m = scores.getMatrix(0) ;
		double score_i = m.getValue(i, 0) ;
		int rank = 1 ;
		for(int j = 0 ; j<m.getNumberOfRows() ; j++) {
			if(m.getValue(j, 0) < score_i)
				rank++ ;
		}
		return rank ;
	}
	
	

	@Override
	public void backward() {
		global.backward_updateGradient(null);

	}
	
	public Parameters getUsedParams(){
		return this.params;
	}
	
	public void updateParams(double line){
		getUsedParams().update(line);
		global.paramsChanged();
	}

	public static void main(String[] args) {
		
		Sequential1 s;
		try {
			
			s = new Sequential1(888,30, "/home/guardiae/AMMICO/flickr/Firenze/train", 50, "/home/guardiae/AMMICO/flickr/Pisa/results/trainModel",0.1,0.0);
			

			/*Trace t = new Trace() ;
			t.add(0) ;
			t.add(1) ;
			t.add(2) ;
			t.add(3) ;
			t.add(4) ;
			s.training_traces=new ArrayList<Trace>() ;
			s.training_traces.add( t) ;*/
			
			s.learn(30000,0.4,0.9999) ;
			s.save() ;
			s.eval("/home/guardiae/AMMICO/flickr/Firenze/test");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			

	}

}