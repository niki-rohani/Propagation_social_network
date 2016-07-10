package propagationModels;

import mlp.CPUAddVecs;
import mlp.CPUAddVals;
import mlp.CPUAverageRows;
import mlp.CPUMatrix;
import mlp.CPUAverageCols;
import mlp.CPUExp;
import mlp.CPUL2Norm;
import mlp.CPULogistic;
import mlp.CPUMatrix;
import mlp.CPUParams;
import mlp.CPUPower;
import mlp.CPUSelectCols;
import mlp.CPUSplit;
import mlp.CPUSum;
import mlp.CPUTanh;
import mlp.CPUTermByTerm;
import mlp.CPUTimesVals;
import mlp.Module;
import mlp.Parameter;
import mlp.Parameters;
import mlp.SequentialModule;
import mlp.TableModule;
import mlp.Tensor;
import mlp.CPUSoftMax;

import java.util.ArrayList;

import mlp.CPUTimesVals;
public abstract class MLPsimFromPoints extends SequentialModule {
	/*SequentialModule trans=null;
	SequentialModule diag=null;
	*/
	Integer nbd;
	TableModule points;
	public MLPsimFromPoints(Integer nbd){
		this.nbd=nbd;
		points=new TableModule();
		points.addModule(null);
		points.addModule(null);
		modules.add(points);
	}
	public void setPoint1(Module p1){
		points.setModule(0,p1);
	}
	public void setPoint2(Module p2){
		points.setModule(1,p2);
	}
	public Module forwardSharedModule()
	{
		throw new RuntimeException("ForwardSharedModule not implemented for "+this);
	}
	public Module parametersSharedModule()
	{
		throw new RuntimeException("ParametersSharedModule not implemented for "+this);
	}
	
	/*public void forward(Tensor input){
		points.getModule(0).forward(input);
		System.out.println(points.getModule(0).getOutput().getMatrix(0).toString());
		super.forward(input);
	}*/
	
	@Override
	public void destroy(){
		super.destroy();
		/*if(trans!=null){
			trans.destroy();
			trans=null;
		}
		if(diag!=null){
			diag.destroy();
			diag=null;
		}*/
	}
	
	@Override
	public int getNbInputMatrix(){
		int nb=super.getNbInputMatrix();
		/*if(trans!=null){
			nb+=trans.getNbInputMatrix();
		}
		if(diag!=null){
			nb+=diag.getNbInputMatrix();
		}*/
		return nb;
	}
	
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
	}
	
	//protected void setSpecificThings(MLPsimFromPointsDouble mod){}
	
	public static void main(String[] args){
		System.out.println(Math.tanh(-1));
	}
	
}

//sigmoid(z1_0+z2_0-(dist(z1,z2)))
class P1Double extends MLPsimFromPoints{
	TableModule biases;
	public P1Double(Integer nbd){
		super(nbd);
		
		ArrayList<Integer> cols=new ArrayList<Integer>();
		cols.add(0);
		ArrayList<Integer> cols2=new ArrayList<Integer>();
		for(int i=1;i<nbd;i++){
			cols2.add(i);
		}
		points=new TableModule();
		SequentialModule s1=new SequentialModule();
		s1.addModule(null);
		s1.addModule(new CPUSelectCols(nbd,cols2));
		points.addModule(s1);
		SequentialModule s2=new SequentialModule();
		s2.addModule(null);
		s2.addModule(new CPUSelectCols(nbd,cols2));
		points.addModule(s2);
		modules.set(0,points);
		
		biases=new TableModule();
		s1=new SequentialModule();
		s1.addModule(null);
		s1.addModule(new CPUSelectCols(nbd,cols));
		biases.addModule(s1);
		s2=new SequentialModule();
		s2.addModule(null);
		s2.addModule(new CPUSelectCols(nbd,cols));
		biases.addModule(s2);
		SequentialModule sb=new SequentialModule();
		sb.addModule(biases);
		sb.addModule(new CPUSum(1,2));
		
				
		TableModule tab=new TableModule();
		SequentialModule seq=new SequentialModule();
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		seq.addModule(new CPUSum(nbd-1,2,w));
		seq.addModule(new CPUL2Norm(nbd-1));
		CPUTimesVals tt=new CPUTimesVals(1,-1.0);
		tt.setName("SimMult");
		seq.addModule(tt);
		
		tab.addModule(seq);
		tab.addModule(sb);
		SequentialModule seq2=new SequentialModule();
		seq2.addModule(tab);
		seq2.addModule(new CPUSum(1,2));
		
		
		modules.add(seq2);
		
		modules.add(new CPULogistic(1));
		
		
	}
	
	public void setPoint1(Module p1){
		((SequentialModule)points.getModule(0)).setModule(0, p1);
		((SequentialModule)biases.getModule(0)).setModule(0, p1.forwardSharedModule());
	}
	public void setPoint2(Module p2){
		((SequentialModule)points.getModule(1)).setModule(0, p2);
		((SequentialModule)biases.getModule(1)).setModule(0, p2.forwardSharedModule());
	}
		
	
	public void destroy(){
		super.destroy();
		biases.destroy();
		biases=null;
		
	}
	public void clearListeners(){
		super.clearListeners();
		biases.clearListeners();
		
		
	}
	
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P1Double ret=new P1Double(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}

//sigmoid(z1_0+z2_0-(p*dist(z1,z2)))
class P2Double extends MLPsimFromPoints{
	TableModule biases;
	CPUParams p;
	public P2Double(Integer nbd){
		super(nbd);
		p=new CPUParams(1,1);
		p.setName("SimPParam");
		
		ArrayList<Integer> cols=new ArrayList<Integer>();
		cols.add(0);
		ArrayList<Integer> cols2=new ArrayList<Integer>();
		for(int i=1;i<nbd;i++){
			cols2.add(i);
		}
		points=new TableModule();
		SequentialModule s1=new SequentialModule();
		s1.addModule(null);
		s1.addModule(new CPUSelectCols(nbd,cols2));
		points.addModule(s1);
		SequentialModule s2=new SequentialModule();
		s2.addModule(null);
		s2.addModule(new CPUSelectCols(nbd,cols2));
		points.addModule(s2);
		modules.set(0,points);
		
		biases=new TableModule();
		s1=new SequentialModule();
		s1.addModule(null);
		s1.addModule(new CPUSelectCols(nbd,cols));
		biases.addModule(s1);
		s2=new SequentialModule();
		s2.addModule(null);
		s2.addModule(new CPUSelectCols(nbd,cols));
		biases.addModule(s2);
		SequentialModule sb=new SequentialModule();
		sb.addModule(biases);
		sb.addModule(new CPUSum(1,2));
		
				
		
		SequentialModule seq=new SequentialModule();
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		seq.addModule(new CPUSum(nbd-1,2,w));
		seq.addModule(new CPUL2Norm(nbd-1));
		CPUTimesVals tt=new CPUTimesVals(1,-1.0);
		tt.setName("SimMult");
		seq.addModule(tt);
		
		CPUTermByTerm mult=new CPUTermByTerm(1);
		tt.setName("multP");
		TableModule tab1=new TableModule();
		SequentialModule multiplie=new SequentialModule();
		multiplie.addModule(tab1);
		multiplie.addModule(mult);
		tab1.addModule(seq);
		tab1.addModule(p);
		
		TableModule tab=new TableModule();
		tab.addModule(multiplie);
		tab.addModule(sb);
		SequentialModule seq2=new SequentialModule();
		seq2.addModule(tab);
		seq2.addModule(new CPUSum(1,2));
		
		
		modules.add(seq2);
		
		modules.add(new CPULogistic(1));
		
		
	}
	
	public void setPoint1(Module p1){
		((SequentialModule)points.getModule(0)).setModule(0, p1);
		((SequentialModule)biases.getModule(0)).setModule(0, p1.forwardSharedModule());
	}
	public void setPoint2(Module p2){
		((SequentialModule)points.getModule(1)).setModule(0, p2);
		((SequentialModule)biases.getModule(1)).setModule(0, p2.forwardSharedModule());
	}
		
	
	public void destroy(){
		super.destroy();
		p.destroy();
		p=null;
		biases.destroy();
		biases=null;
		
	}
	public void clearListeners(){
		super.clearListeners();
		p.clearListeners();
		biases.clearListeners();
	}
	public int getNbParams(){
		//System.out.println(p.getNbParams()+" params requis");
		return p.getNbParams();
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		ret.addSubParamList(p.getParamList());
		/*for(Parameter p:ret.getParams()){
			System.out.print(" p = "+p.getVal());
			
		}
		System.out.println();*/
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		//System.out.println("size plist = "+pList.size());
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
		ArrayList<Parameter> pars=pList.getParams();
		int n1=p.getNbParams();
		Parameters plist1=new Parameters(n1);
		
		if(pars.size()<n1){
			throw new RuntimeException("Not Enough Parameters");
		}
		for(int i=0;i<n1;i++){
			Parameter par=pars.get(i);
			//p.setVal(-1.0);
			plist1.set(i,par);
			System.out.println("p1="+par.getVal());
			
		}
		
		p.setParameters(plist1);
		
		
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P2Double ret=new P2Double(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.p=(CPUParams)p.forwardSharedModule();
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
	

	
}

//sigmoid(-z1_0-z2_0+<z1,z2>))
class P3Double extends MLPsimFromPoints{
	TableModule biases;
	
	public P3Double(Integer nbd){
		super(nbd);
		
		ArrayList<Integer> cols=new ArrayList<Integer>();
		cols.add(0);
		ArrayList<Integer> cols2=new ArrayList<Integer>();
		for(int i=1;i<nbd;i++){
			cols2.add(i);
		}
		points=new TableModule();
		SequentialModule s1=new SequentialModule();
		s1.addModule(null);
		s1.addModule(new CPUSelectCols(nbd,cols2));
		points.addModule(s1);
		SequentialModule s2=new SequentialModule();
		s2.addModule(null);
		s2.addModule(new CPUSelectCols(nbd,cols2));
		points.addModule(s2);
		modules.set(0,points);
		
		biases=new TableModule();
		s1=new SequentialModule();
		s1.addModule(null);
		s1.addModule(new CPUSelectCols(nbd,cols));
		biases.addModule(s1);
		s2=new SequentialModule();
		s2.addModule(null);
		s2.addModule(new CPUSelectCols(nbd,cols));
		biases.addModule(s2);
		SequentialModule sb=new SequentialModule();
		sb.addModule(biases);
		sb.addModule(new CPUSum(1,2));
		
				
		TableModule tab=new TableModule();
		SequentialModule seq=new SequentialModule();
		
		seq.addModule(new CPUTermByTerm(nbd-1));
		seq.addModule(new CPUAverageCols(nbd-1,2));
		//seq.addModule(new CPUL2Norm(nbd-1));
		//seq.addModule(new CPUPower(1,2));
		
		
		tab.addModule(seq);
		tab.addModule(sb);
		SequentialModule seq2=new SequentialModule();
		seq2.addModule(tab);
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		seq2.addModule(new CPUSum(1,2,w));
		
		
		modules.add(seq2);
		
		modules.add(new CPULogistic(1));
		
		
	}
	
	public void setPoint1(Module p1){
		((SequentialModule)points.getModule(0)).setModule(0, p1);
		((SequentialModule)biases.getModule(0)).setModule(0, p1.forwardSharedModule());
	}
	public void setPoint2(Module p2){
		((SequentialModule)points.getModule(1)).setModule(0, p2);
		((SequentialModule)biases.getModule(1)).setModule(0, p2.forwardSharedModule());
	}
		
	
	public void destroy(){
		super.destroy();
		biases.destroy();
		biases=null;
		
	}
	public void clearListeners(){
		super.clearListeners();
		biases.clearListeners();
		
		
	}
	
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P3Double ret=new P3Double(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}



//sigmoid(z1_0+z2_0+p*(z1.z2)))
class P4Double extends MLPsimFromPoints{
	TableModule biases;
	CPUParams p;
	public P4Double(Integer nbd){
		super(nbd);
		p=new CPUParams(1,1);
		p.setName("SimPParam");
		
		ArrayList<Integer> cols=new ArrayList<Integer>();
		cols.add(0);
		ArrayList<Integer> cols2=new ArrayList<Integer>();
		for(int i=1;i<nbd;i++){
			cols2.add(i);
		}
		points=new TableModule();
		SequentialModule s1=new SequentialModule();
		s1.addModule(null);
		s1.addModule(new CPUSelectCols(nbd,cols2));
		points.addModule(s1);
		SequentialModule s2=new SequentialModule();
		s2.addModule(null);
		s2.addModule(new CPUSelectCols(nbd,cols2));
		points.addModule(s2);
		modules.set(0,points);
		
		biases=new TableModule();
		s1=new SequentialModule();
		s1.addModule(null);
		s1.addModule(new CPUSelectCols(nbd,cols));
		biases.addModule(s1);
		s2=new SequentialModule();
		s2.addModule(null);
		s2.addModule(new CPUSelectCols(nbd,cols));
		biases.addModule(s2);
		SequentialModule sb=new SequentialModule();
		sb.addModule(biases);
		sb.addModule(new CPUSum(1,2));
		
		
		
				
		TableModule tab=new TableModule();
		SequentialModule seq=new SequentialModule();
		
		seq.addModule(new CPUTermByTerm(nbd-1));
		seq.addModule(new CPUAverageCols(nbd-1,2));
		//seq.addModule(new CPUL2Norm(nbd-1));
		//seq.addModule(new CPUPower(1,2));
		
		CPUTermByTerm mult=new CPUTermByTerm(1);
		mult.setName("multP");
		TableModule tab1=new TableModule();
		SequentialModule multiplie=new SequentialModule();
		multiplie.addModule(tab1);
		multiplie.addModule(mult);
		tab1.addModule(seq);
		tab1.addModule(p);
		
		
		tab.addModule(multiplie);
		tab.addModule(sb);
		SequentialModule seq2=new SequentialModule();
		seq2.addModule(tab);
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		seq2.addModule(new CPUSum(1,2,w));
		
		
		modules.add(seq2);
		
		modules.add(new CPULogistic(1));
		
		
	}
	
	public void setPoint1(Module p1){
		((SequentialModule)points.getModule(0)).setModule(0, p1);
		((SequentialModule)biases.getModule(0)).setModule(0, p1.forwardSharedModule());
	}
	public void setPoint2(Module p2){
		((SequentialModule)points.getModule(1)).setModule(0, p2);
		((SequentialModule)biases.getModule(1)).setModule(0, p2.forwardSharedModule());
	}
		
	
	public void destroy(){
		super.destroy();
		p.destroy();
		p=null;
		biases.destroy();
		biases=null;
		
	}
	public void clearListeners(){
		super.clearListeners();
		p.clearListeners();
		biases.clearListeners();
		
		
	}
	public int getNbParams(){
		//System.out.println(p.getNbParams()+" params requis");
		return p.getNbParams();
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		ret.addSubParamList(p.getParamList());
		/*for(Parameter p:ret.getParams()){
			System.out.print(" p = "+p.getVal());
			
		}
		System.out.println();*/
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		//System.out.println("size plist = "+pList.size());
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
		ArrayList<Parameter> pars=pList.getParams();
		int n1=p.getNbParams();
		Parameters plist1=new Parameters(n1);
		
		if(pars.size()<n1){
			throw new RuntimeException("Not Enough Parameters");
		}
		for(int i=0;i<n1;i++){
			Parameter par=pars.get(i);
			//p.setVal(-1.0);
			plist1.set(i,par);
			System.out.println("p1="+par.getVal());
			
		}
		
		p.setParameters(plist1);
		
		
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P4Double ret=new P4Double(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.p=(CPUParams)p.forwardSharedModule();
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}



// sigmoid(p1+(p2*dist(z1,z2)))
class P5Double extends MLPsimFromPoints{
	CPUParams p1;
	CPUParams p2;
	
	public P5Double(Integer nbd){
		super(nbd);
		p1=new CPUParams(1,1);
		p1.setName("SimPParam1");
		p2=new CPUParams(1,1);
		p2.setName("SimPParam2");
		
		CPUAddVecs add=new CPUAddVecs(1);
		add.setName("SimAdd");
		CPUTermByTerm tt=new CPUTermByTerm(1);
		tt.setName("SimMult");
		
		SequentialModule seq=new SequentialModule();
		TableModule tab=new TableModule();
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		seq.addModule(new CPUSum(nbd,2,w));
		seq.addModule(new CPUL2Norm(nbd));
		tab.addModule(seq);
		tab.addModule(p2);
		SequentialModule seq2=new SequentialModule();
		seq2.addModule(tab);
		seq2.addModule(tt);
		
		TableModule tab2=new TableModule();
		tab2.addModule(seq2);
		tab2.addModule(p1);
		modules.add(tab2);
		
		modules.add(add);
		modules.add(new CPULogistic(1));
		
		p1.paramsChanged();
		p2.paramsChanged();
		
	}
	
	
		
	
	public void destroy(){
		super.destroy();
		p1.destroy();
		p2.destroy();
		p1=null;
		p2=null;
		
	}
	public void clearListeners(){
		super.clearListeners();
		p1.clearListeners();
		p2.clearListeners();
		
	}
	public int getNbParams(){
		return p1.getNbParams()+p2.getNbParams();
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		ret.addSubParamList(p1.getParamList());
		ret.addSubParamList(p2.getParamList());
		/*for(Parameter p:ret.getParams()){
			System.out.print(" p = "+p.getVal());
			
		}
		System.out.println();*/
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
		ArrayList<Parameter> pars=pList.getParams();
		int n1=p1.getNbParams();
		int n2=p2.getNbParams();
		Parameters plist1=new Parameters(n1);
		Parameters plist2=new Parameters(n2);
		
		if(pars.size()<=n1){
			throw new RuntimeException("Not Enough Parameters");
		}
		for(int i=0;i<n1;i++){
			Parameter p=pars.get(i);
			//p.setVal(-1.0);
			plist1.set(i,p);
			System.out.println("p1="+p.getVal());
			
		}
		for(int i=n1;i<pars.size();i++){
			Parameter p=pars.get(i);
			p.setUpperBound(-0.01);
			//p.setVal(1.0);
			plist2.set(i-n1,p);
			System.out.println("p2="+p.getVal());
		}
		p1.setParameters(plist1);
		p2.setParameters(plist2);
		//p1.lockParams();
		//p2.lockParams();
		
		
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P5Double ret=new P5Double(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.p1=(CPUParams)p1.forwardSharedModule();
		ret.p2=(CPUParams)p2.forwardSharedModule();
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}

//1-tanh(z1_0^2+z2_0^2+(p*dist(z1,z2)))
class P6Double extends MLPsimFromPoints{
	TableModule biases;
	CPUParams p;
	public P6Double(Integer nbd){
		super(nbd);
		p=new CPUParams(1,1);
		p.setName("SimPParam");
		
		ArrayList<Integer> cols=new ArrayList<Integer>();
		cols.add(0);
		ArrayList<Integer> cols2=new ArrayList<Integer>();
		for(int i=1;i<nbd;i++){
			cols2.add(i);
		}
		points=new TableModule();
		SequentialModule s1=new SequentialModule();
		s1.addModule(null);
		s1.addModule(new CPUSelectCols(nbd,cols2));
		points.addModule(s1);
		SequentialModule s2=new SequentialModule();
		s2.addModule(null);
		s2.addModule(new CPUSelectCols(nbd,cols2));
		points.addModule(s2);
		modules.set(0,points);
		
		biases=new TableModule();
		s1=new SequentialModule();
		s1.addModule(null);
		s1.addModule(new CPUSelectCols(nbd,cols));
		s1.addModule(new CPUPower(1,2));
		biases.addModule(s1);
		s2=new SequentialModule();
		s2.addModule(null);
		s2.addModule(new CPUSelectCols(nbd,cols));
		s2.addModule(new CPUPower(1,2));
		biases.addModule(s2);
		SequentialModule sb=new SequentialModule();
		sb.addModule(biases);
		sb.addModule(new CPUSum(1,2));
		
				
		
		SequentialModule seq=new SequentialModule();
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		seq.addModule(new CPUSum(nbd-1,2,w));
		seq.addModule(new CPUL2Norm(nbd-1));
		//CPUTimesVals tt=new CPUTimesVals(1,-1.0);
		//tt.setName("SimMult");
		//seq.addModule(tt);
		
		CPUTermByTerm mult=new CPUTermByTerm(1);
		mult.setName("multP");
		TableModule tab1=new TableModule();
		SequentialModule multiplie=new SequentialModule();
		multiplie.addModule(tab1);
		multiplie.addModule(mult);
		tab1.addModule(seq);
		tab1.addModule(p);
		
		TableModule tab=new TableModule();
		tab.addModule(multiplie);
		tab.addModule(sb);
		SequentialModule seq2=new SequentialModule();
		seq2.addModule(tab);
		seq2.addModule(new CPUSum(1,2));
		
		
		modules.add(seq2);
		
		modules.add(new CPUTanh(1,1.0,1.0));
		CPUAddVals add=new CPUAddVals(1,1.0);
		add.setName("SimAdd");
		CPUTimesVals ti=new CPUTimesVals(1,-1.0);
		ti.setName("neg");
		modules.add(ti);
		modules.add(add);
	}
	
	public void setPoint1(Module p1){
		((SequentialModule)points.getModule(0)).setModule(0, p1);
		((SequentialModule)biases.getModule(0)).setModule(0, p1.forwardSharedModule());
	}
	public void setPoint2(Module p2){
		((SequentialModule)points.getModule(1)).setModule(0, p2);
		((SequentialModule)biases.getModule(1)).setModule(0, p2.forwardSharedModule());
	}
		
	
	public void destroy(){
		super.destroy();
		p.destroy();
		p=null;
		biases.destroy();
		biases=null;
		
	}
	public void clearListeners(){
		super.clearListeners();
		p.clearListeners();
		biases.clearListeners();
	}
	public int getNbParams(){
		//System.out.println(p.getNbParams()+" params requis");
		return p.getNbParams();
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		ret.addSubParamList(p.getParamList());
		/*for(Parameter p:ret.getParams()){
			System.out.print(" p = "+p.getVal());
			
		}
		System.out.println();*/
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		//System.out.println("size plist = "+pList.size());
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
		ArrayList<Parameter> pars=pList.getParams();
		int n1=p.getNbParams();
		Parameters plist1=new Parameters(n1);
		
		if(pars.size()<n1){
			throw new RuntimeException("Not Enough Parameters");
		}
		for(int i=0;i<n1;i++){
			Parameter par=pars.get(i);
			//p.setVal(-1.0);
			plist1.set(i,par);
			System.out.println("p1="+par.getVal());
			
		}
		
		p.setParameters(plist1);
		
		
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P6Double ret=new P6Double(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.p=(CPUParams)p.forwardSharedModule();
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
	

	
}


//sigmoid(alpha+(beta*dist(z1,z2)))
class P7Double extends MLPsimFromPoints{
	double alpha;
	double beta;
	public P7Double(Integer nbd,double alpha, double beta){
		super(nbd);
		
		CPUAddVals add=new CPUAddVals(1,alpha);
		add.setName("SimAdd");
		CPUTimesVals tt=new CPUTimesVals(1,beta);
		tt.setName("SimMult");
		
		SequentialModule seq=new SequentialModule();
		TableModule tab=new TableModule();
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		modules.add(new CPUSum(nbd,2,w));
		modules.add(new CPUL2Norm(nbd));
		modules.add(tt);
		
		modules.add(add);
		
		modules.add(new CPULogistic(1));
		
		
		
	}
	
	
		
	
	public void destroy(){
		super.destroy();
		
		
	}
	public void clearListeners(){
		super.clearListeners();
		
		
	}
	public int getNbParams(){
		return 0;
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P7Double ret=new P7Double(this.nbd,alpha,beta);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}


//1-tanh(p*dist(z1,z2))
class P8Double extends MLPsimFromPoints{
	CPUParams p;
	public P8Double(Integer nbd){
		super(nbd);
		p=new CPUParams(1,1);
		p.setName("SimPParam");

		
		SequentialModule seq=new SequentialModule();
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		seq.addModule(new CPUSum(nbd,2,w));
		seq.addModule(new CPUL2Norm(nbd));
		//CPUTimesVals tt=new CPUTimesVals(1,-1.0);
		//tt.setName("SimMult");
		//seq.addModule(tt);
		
		CPUTermByTerm mult=new CPUTermByTerm(1);
		mult.setName("multP");
		TableModule tab1=new TableModule();
		SequentialModule multiplie=new SequentialModule();
		multiplie.addModule(tab1);
		multiplie.addModule(mult);
		tab1.addModule(seq);
		tab1.addModule(p);
		
		
		
		
		modules.add(multiplie);
		
		modules.add(new CPUTanh(1,1.0,1.0));
		CPUAddVals add=new CPUAddVals(1,1.0);
		add.setName("SimAdd");
		CPUTimesVals ti=new CPUTimesVals(1,-1.0);
		ti.setName("neg");
		modules.add(ti);
		modules.add(add);
	}
	
	
		
	
	public void destroy(){
		super.destroy();
		p.destroy();
		p=null;
		
		
	}
	public void clearListeners(){
		super.clearListeners();
		p.clearListeners();
		
	}
	public int getNbParams(){
		//System.out.println(p.getNbParams()+" params requis");
		return p.getNbParams();
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		ret.addSubParamList(p.getParamList());
		/*for(Parameter p:ret.getParams()){
			System.out.print(" p = "+p.getVal());
			
		}
		System.out.println();*/
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		//System.out.println("size plist = "+pList.size());
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
		ArrayList<Parameter> pars=pList.getParams();
		int n1=p.getNbParams();
		Parameters plist1=new Parameters(n1);
		
		if(pars.size()<n1){
			throw new RuntimeException("Not Enough Parameters");
		}
		for(int i=0;i<n1;i++){
			Parameter par=pars.get(i);
			//p.setVal(-1.0);
			plist1.set(i,par);
			System.out.println("p1="+par.getVal());
			
		}
		
		p.setParameters(plist1);
		
		
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P8Double ret=new P8Double(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.p=(CPUParams)p.forwardSharedModule();
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
	

	
}

//sigmoid(a+(b*softmax_{i}((zi1-zi2)^2,alpha)))
class P9 extends MLPsimFromPoints{
	double alpha;
	double a;
	double b;
	public P9(Integer nbd,double a, double b,double alpha){
		super(nbd);
		this.a=a;
		this.b=b;
		this.alpha=alpha;
		CPUAddVals add=new CPUAddVals(1,a);
		add.setName("SimAdd");
		CPUTimesVals tt=new CPUTimesVals(1,b);
		tt.setName("SimMult");
		
		SequentialModule seq=new SequentialModule();
		TableModule tab=new TableModule();
		ArrayList<Double> w=new ArrayList<Double>();
		w.add(1.0); w.add(-1.0); 
		modules.add(new CPUSum(nbd,2,w));
		modules.add(new CPUPower(nbd,2));
		modules.add(new CPUSoftMax(nbd,alpha,1));
		
		modules.add(tt);
		
		modules.add(add);
		
		modules.add(new CPULogistic(1));
		
		
		
	}
	
	
		
	
	public void destroy(){
		super.destroy();
		
		
	}
	public void clearListeners(){
		super.clearListeners();
		
		
	}
	public int getNbParams(){
		return 0;
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P9 ret=new P9(this.nbd,a,b,alpha);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}



//sigmoid(alpha+(beta*||(z1*z2)||^2))
class P10Double extends MLPsimFromPoints{
	double alpha;
	double beta;
	public P10Double(Integer nbd,double alpha, double beta){
		super(nbd);
		
		CPUAddVals add=new CPUAddVals(1,alpha);
		add.setName("SimAdd");
		CPUTimesVals tt=new CPUTimesVals(1,beta);
		tt.setName("SimMult");
		
		SequentialModule seq=new SequentialModule();
		TableModule tab=new TableModule();
		modules.add(new CPUTermByTerm(nbd)); 
		modules.add(new CPUL2Norm(nbd));
		modules.add(tt);
		
		
		
		modules.add(add);
		
		modules.add(new CPULogistic(1));
		
		
		
	}
	
	
		
	
	public void destroy(){
		super.destroy();
		
		
	}
	public void clearListeners(){
		super.clearListeners();
		
		
	}
	public int getNbParams(){
		return 0;
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P10Double ret=new P10Double(this.nbd,alpha,beta);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}

//sigmoid(alpha+(beta*(z1.z2)^2))
class P11Double extends MLPsimFromPoints{
	double alpha;
	double beta;
	public P11Double(Integer nbd,double alpha, double beta){
		super(nbd);
		
		CPUAddVals add=new CPUAddVals(1,alpha);
		add.setName("SimAdd");
		CPUTimesVals tt=new CPUTimesVals(1,beta);
		tt.setName("SimMult");
		
		SequentialModule seq=new SequentialModule();
		TableModule tab=new TableModule();
		modules.add(new CPUTermByTerm(nbd)); 
		modules.add(new CPUAverageCols(nbd,2));
		//modules.add(new CPUAddVals(1,1));
		//modules.add(new CPUPower(1,2));
		
		modules.add(tt);
		
		
		
		modules.add(add);
		
		modules.add(new CPULogistic(1));
		
		
		
	}
	
	
		
	
	public void destroy(){
		super.destroy();
		
		
	}
	public void clearListeners(){
		super.clearListeners();
		
		
	}
	public int getNbParams(){
		return 0;
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P11Double ret=new P11Double(this.nbd,alpha,beta);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}

//sigmoid(-(z1[0..n/2].z2[0..n/2])+(z1[n/2..n].z2[n/2..n])))
class P13Double extends MLPsimFromPoints{
	
	public P13Double(Integer nbd){
		super(nbd);
		//SequentialModule seq=new SequentialModule();
		//TableModule tab=new TableModule();
		modules.add(new CPUTermByTerm(nbd)); 
		//modules.add(new CPUPower(nbd,2));
		CPUMatrix mat=new CPUMatrix(1,nbd,1.0);
		for(int i=0;i<nbd/2;i++){
			mat.setValue(0, i, -1.0);
		}
		CPUTimesVals tt=new CPUTimesVals(nbd,mat);
		tt.setName("SimMult");
		modules.add(tt);
		modules.add(new CPUAverageCols(nbd,2));
		modules.add(new CPULogistic(1));
	}
	
	
		
	
	public void destroy(){
		super.destroy();
		
		
	}
	public void clearListeners(){
		super.clearListeners();
		
		
	}
	public int getNbParams(){
		return 0;
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P13Double ret=new P13Double(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}

//1-tanh(z1.z2)
class P14Double extends MLPsimFromPoints{
	
	public P14Double(Integer nbd){
		super(nbd);
		//SequentialModule seq=new SequentialModule();
		//TableModule tab=new TableModule();
		modules.add(new CPUTermByTerm(nbd)); 
		//modules.add(new CPUPower(nbd,2));
		modules.add(new CPUAverageCols(nbd,2));
		modules.add(new CPUTanh(1,1.0,1.0));
		CPUAddVals add=new CPUAddVals(1,1.0);
		add.setName("SimAdd");
		CPUTimesVals ti=new CPUTimesVals(1,-1.0);
		ti.setName("neg");
		modules.add(ti);
		modules.add(add);
	}
	
	
		
	
	public void destroy(){
		super.destroy();
		
		
	}
	public void clearListeners(){
		super.clearListeners();
		
		
	}
	public int getNbParams(){
		return 0;
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P14Double ret=new P14Double(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}

//tanh(z1.z2)
class P15Double extends MLPsimFromPoints{
	
	public P15Double(Integer nbd){
		super(nbd);
		//SequentialModule seq=new SequentialModule();
		//TableModule tab=new TableModule();
		modules.add(new CPUTermByTerm(nbd)); 
		//modules.add(new CPUPower(nbd,2));
		modules.add(new CPUAverageCols(nbd,2));
		modules.add(new CPUTanh(1,1.0,0.1));
		
	}
	
	
		
	
	public void destroy(){
		super.destroy();
		
		
	}
	public void clearListeners(){
		super.clearListeners();
		
		
	}
	public int getNbParams(){
		return 0;
	}
	public Parameters getParamList(){
		Parameters ret=new Parameters();
		return(ret);
	}
	public void setParameters(Parameters pList)
	{
		if(sharedForward){
			throw new RuntimeException(this+": Please not call setParameters on a shared forward module");
		}
		if(getListeners().size()>0){
			throw new RuntimeException(this+": Please not call setParameters on a module that shares its parameters");
		}
	}
	
	
	
	public Module forwardSharedModule()
	{
		if(sharedForward){
			return origin_module.forwardSharedModule();
		}
		P15Double ret=new P15Double(this.nbd);
		addListener(ret);
		ret.sharedForward=true;
		ret.origin_module=this;
		ret.modules.clear();
		for(Module mod:modules){
			Module m=mod.forwardSharedModule();
			ret.modules.add(m);
		}
		return(ret);
	}
	
}






