package mlp;
//import java.util.TreeMap;
import java.util.ArrayList;
import java.util.HashSet;
public class Parameters {
	private ArrayList<Parameters> subParameterLists;
	//private ArrayList<Parameter> params;
	private Parameter[] params;
	//private Version version=new Version(0);
	//private Parameters parent;
	//public HashSet<Module> listeners;
	public Parameters(){
		//params=new ArrayList<Parameter>();
		params=new Parameter[0];
		
		//this.listeners=new HashSet<Module>();
		subParameterLists=new ArrayList<Parameters>();
		/*parent=null;
		version=new Version(0);*/
	}
	
	public Parameters(int nb){
		//params=new ArrayList<Parameter>(nb);
		params=new Parameter[nb];
		//version=new Version(0);
		subParameterLists=new ArrayList<Parameters>();
		
		//parent=null;
	}
	public Parameters(int nb,double val,double lowerBound,double upperBound){
		//params=new ArrayList<Parameter>();
		params=new Parameter[nb];
		
		//version=new Version(0);
		subParameterLists=new ArrayList<Parameters>();
		for(int i=0;i<nb;i++){
			//params.add(new Parameter(val,lowerBound,upperBound));
			params[i]=(new Parameter(val,lowerBound,upperBound));
		}
		//parent=null;
	}
	public Parameters(int nb,double val){
		this(nb,val,-Double.MAX_VALUE,Double.MAX_VALUE);
	}
	
	// Pour sauvegarde params uniquement
	// Uniquement a partir de la racine
	Parameters(Parameters pars){
		/*this(pars,true);
	}
	private Parameters(Parameters pars,boolean checkRoot){
		if(checkRoot && pars.parent!=null){
			throw new RuntimeException("Parameters can only be saved from the root");
		}
		version=pars.version;*/
		//params=new ArrayList<Parameter>();
		Parameter[] par=pars.params;
		params=new Parameter[par.length];
		
		for(int i=0;i<par.length;i++){
			Parameter cp=new Parameter(par[i]);
			params[i]=cp;
			
			//cp.parent=this;
		}
		//this.listeners=pars.listeners;
		subParameterLists=new ArrayList<Parameters>();
		for(Parameters p:pars.subParameterLists){
			Parameters np=new Parameters(p); //,false);
			this.subParameterLists.add(np);
			//np.parent=this;
		}
	}
	
	
	// Pour restoration de params uniquement
	// Uniquement a partir de la racine
	void restoreParams(Parameters pars){
		/*restoreParams(pars,true);
	}
	private void restoreParams(Parameters pars,boolean checkRoot){
		if(checkRoot && this.parent!=null){
			throw new RuntimeException("Parameters can only be saved from the root");
		}
		if(checkRoot && pars.parent!=null){
			throw new RuntimeException("Parameters can only be saved from the root");
		}
		version.version=pars.version.version;*/
		Parameter[] par=pars.params;
		int i=0;
		for(Parameter p:params){
			Parameter pi=par[i];
			p.setVal(pi.getVal());
			p.gradient=0.0f;
			p.last_gradient=0.0f;
        	p.last_direction=0.0f;
        	p.direction=0.0f;
        	//p.version=pi.version;
			i++;
		}
		i=0;
		for(Parameters p:subParameterLists){
			p.restoreParams(pars.subParameterLists.get(i)); //,false);
			i++;
		}
		//paramsChanged();
	}
	
	public void revertLastMove(){
		for(Parameter p:params){
			p.setVal(p.last_val);
		}
		for(Parameters p:this.subParameterLists){
			p.revertLastMove(); //,v);
		}
	}
	
	/*public void addParam(Parameter p){
		params.add(p);
		//p.parent=this;
	}*/
	
	public int size(){
		int s=params.length;
		for(Parameters p:subParameterLists){
			s+=p.size();
		}
		return s;
	}
	public int length(){
		int s=params.length;
		
		return s;
	}
	
	public Parameter get(int i){
		if(i<params.length){
			return params[i];
		}
		else{
			i-=params.length;
			int x=i;
			for(Parameters pr:subParameterLists){
				x-=pr.size();
				if(x<0){
					x+=pr.size();
					return pr.get(x);
				}
			}
		}
		throw new RuntimeException("No parameter i (only "+size()+")");
		//return null;
	}
	public void set(int i, Parameter p){
		if(i<params.length){
			params[i]=p;
		}
		else{
			i-=params.length;
			int x=i;
			boolean ok=false;
			for(Parameters pr:subParameterLists){
				x-=pr.size();
				if(x<0){
					x+=pr.size();
					pr.set(x, p);
					ok=true;
					break;
				}
			}
			if(!ok) throw new RuntimeException("No parameter i (only "+size()+")");
		}
	}
	
	/*public void addListener(Module f){
		listeners.add(f);
	}
	public void removeListener(Module f){
		listeners.remove(f);
	}
	public void clearListeners(){
		this.listeners=new HashSet<Module>();
	}*/
	
	/*private void paramsChanged(){
		for(Module f:listeners){
			f.paramsChanged();
		}
	}*/
	
	/*public void update(double line){
		propagateUpVersion();
		update(line,version.version+1);
	}
	private void propagateUpVersion(){
		this.parent.version.version++;
		this.parent.propagateUpVersion();
	}*/
	
	public void update(double line){ //,int v){
		//HashSet<Parameter> vus=new HashSet<Parameter>();
		//version.version=v;
		for(Parameter p:params){
			/*if(vus.contains(p)){
				continue;
			}
			vus.add(p);*/
			/*if(p.version==v){
				continue;
			}
			p.version=v;*/
			if(p==null){
				System.out.println("P null");
				continue;
			}
			double nv=p.getVal()+line*p.direction;
			
			//double ln=line;
			if(Double.isNaN(nv)){
				
				throw new RuntimeException("Parameter has NaN val => line = "+line+" val = "+p.getVal()+" direction = "+p.direction);
				/*ln=ln/10.0f;
				nv=p.getVal()+ln*p.direction;*/
			}
			if(Double.isInfinite(nv)){
				if(nv>0){
					nv=Double.MAX_VALUE;
				}
				else{
					nv=-Double.MAX_VALUE;
				}
				System.out.println("Parameter has infinite value => line = "+line+" val = "+p.getVal()+" direction = "+p.direction+" new val = "+nv);
				
			}
			p.setVal(nv);
			
		}
		
		for(Parameters p:this.subParameterLists){
			p.update(line); //,v);
		}
		//paramsChanged();
	}
	
	public void setMaxMove(double maxMove){
		for(Parameter param:params){
			param.setMaxMove(maxMove);
		}
		for(Parameters p:this.subParameterLists){
			p.setMaxMove(maxMove);
		}
	}
	
	/*public Parameter get(int i){
		return(params.get(i));
	}*/
	
	public double computeDotDirectionGradient(){
		double sum=0.0f;
		for(Parameter p:params){
			sum+=p.direction*p.gradient;
		}
		for(Parameters p:this.subParameterLists){
			sum+=p.computeDotDirectionGradient();
		}
		return(sum);
	}
	public ArrayList<Parameter> getParams(){
		ArrayList<Parameter> pars=new ArrayList<Parameter>();
		for(Parameter p:params){
			pars.add(p);
		}
		for(Parameters p:this.subParameterLists){
			pars.addAll(p.getParams());
		}
		return(pars);
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		ArrayList<Parameter> pars=getParams();
		sb.append("Param Values : \n");
		int i=0;
		for(Parameter p:pars){
			sb.append(i+"="+p.getVal()+",");
			i++;
		}
		sb.append("\nGradients : \n");
		i=0;
		for(Parameter p:pars){
			sb.append(i+"="+p.gradient+",");
			i++;
		}
		
		sb.append("\nDirection : \n");
		i=0;
		for(Parameter p:pars){
			sb.append(i+"="+p.direction+",");
			i++;
		}
		return(sb.toString());
	}
	
	/*public void computeGradient(MLPModel mod){
		mod.backward();
		
	}*/
	
	public void giveAllParamsTo(Module mod){
		ArrayList<Parameter> para=getParams();
		Parameters pars=new Parameters(this.size());
		for(int i=0;i<para.size();i++){
			pars.set(i,para.get(i));
		}
		//this.addListener(mod);
		mod.setParameters(pars);
		mod.paramsChanged();
	}
	
	public void adjustParameters(int nbPars){
		adjustParameters(nbPars,-Double.MAX_VALUE,Double.MAX_VALUE);
	}
	
	public void adjustParameters(int nbPars,double lowerBound,double upperBound){
		Parameter[] nparams=new Parameter[nbPars];
		int nbd=nbPars;
		if(params.length<nbd){
			nbd=params.length;
		}
		for(int i=0;i<nbd;i++){
			nparams[i]=params[i];
		}
		int i=nbd;
		while(i<nbPars){
			Parameter np=new Parameter(((double)(Math.random())),lowerBound,upperBound);
			nparams[i]=np;
			i++;
		}
		params=nparams;
	}
	
	public void allocateNewParamsFor(Module mod){
		allocateNewParamsFor(mod,-Double.MAX_VALUE,Double.MAX_VALUE);
	}
	
	public void allocateNewParamsFor(Module mod,double lowerBound,double upperBound){
		int nb=mod.getNbParams();
		//int nb=mod.input_size*mod.output_size;
		Parameters para=new Parameters(nb);
		//Parameter[] pars=new Parameter[nb];
		for(int i=0;i<nb;i++){
			Parameter np=new Parameter(((double)(Math.random())),lowerBound,upperBound);
			
			
			//pars[i]=np;
			para.params[i]=np;
			//np.parent=para;
		}
		this.subParameterLists.add(para);
		mod.setParameters(para);
		mod.paramsChanged();
	}
	
	public void allocateNewParamsFor(Module mod, double minInitVal, double maxInitVal, double lowerBound,double upperBound){
		int nb=mod.getNbParams();
		//int nb=mod.input_size*mod.output_size;
		Parameters para=new Parameters(nb);
		//Parameter[] pars=new Parameter[nb];
		double delta=maxInitVal-minInitVal;
		for(int i=0;i<nb;i++){
			Parameter np=new Parameter(((double)((Math.random()*delta)+minInitVal)),lowerBound,upperBound);
			
			
			//pars[i]=np;
			para.params[i]=np;
			//np.parent=para;
		}
		this.subParameterLists.add(para);
		mod.setParameters(para);
		mod.paramsChanged();
	}
	
	public void allocateNewParamsFor(Module mod,double[] vals){
		allocateNewParamsFor(mod,vals,-Double.MAX_VALUE,Double.MAX_VALUE);
	}
	
	public void allocateNewParamsFor(Module mod,double[] vals,double lowerBound,double upperBound){
		int nb=mod.getNbParams();
		//int nb=mod.input_size*mod.output_size;
		Parameters para=new Parameters(nb);
		//Parameter[] pars=new Parameter[nb];
		for(int i=0;i<nb;i++){
			Parameter np=new Parameter(vals[i],lowerBound,upperBound);
			
			//pars[i]=np;
			para.params[i]=np;
			//np.parent=para;
		}
		this.subParameterLists.add(para);
		mod.setParameters(para);
		mod.paramsChanged();
	}
	
	public void allocateNewParamsFor(Module mod,double val){
		allocateNewParamsFor(mod,val,-Double.MAX_VALUE,Double.MAX_VALUE);
	}
	
	public void allocateNewParamsFor(Module mod,double val,double lowerBound,double upperBound){
		int nb=mod.getNbParams();
		//int nb=mod.input_size*mod.output_size;
		Parameters para=new Parameters(nb);
		//Parameter[] pars=new Parameter[nb];
		for(int i=0;i<nb;i++){
			Parameter np=new Parameter(val,lowerBound,upperBound);
			
			//pars[i]=np;
			para.params[i]=np;
			//np.parent=para;
		}
		this.subParameterLists.add(para);
		mod.setParameters(para);
		mod.paramsChanged();
	}
	
	public void addSubParamList(Parameters p){
		this.subParameterLists.add(p);
	}
}

class Version{
	int version=0;
	Version(int x){
		version=x;
	}
}