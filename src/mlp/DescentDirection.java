package mlp;

import java.util.HashMap;
import java.util.ArrayList;
import core.Utils;

public abstract class DescentDirection {
	//protected Parameter[] params;
    protected int nb; // nombre d'appels de getDirection

    public DescentDirection() {
            
            nb=0;
     }
     public abstract void computeDirection(MLPModel loss); //Parameters params);
     /*public void setFonction(Fonction _fonc){
            fonc=_fonc;
            lastDirection=new HashMap<Integer,Double>();
            lastGradient=new HashMap<Integer,Double>();
     }
     public void reset(){
        	lastDirection=new HashMap<Integer,Double>();
            lastGradient=new HashMap<Integer,Double>();
     }*/
    
     public static DescentDirection getGradientDirection(){
    	 return new GradientDirection();
     }
     public static DescentDirection getAverageGradientDirection(){
    	 return new AverageGradientDirection();
     }
     public static DescentDirection getGradientSignDirection(){
    	 return new GradientSignDirection();
     }
     public static DescentDirection getBenjaminGradientDirection(double lambda){
    	 return new BenjaminGradientDirection(lambda);
     }
     public static DescentDirection getPR_ConjugateGradientDirection(){
    	 return new PR_ConjugateGradientDirection();
     }
}

class GradientDirection extends DescentDirection
{
    public void computeDirection(MLPModel loss){
            nb++;
            Parameters params=loss.getUsedParams();
            for(Parameter p:params.getParams()){
            	
            	
            	p.last_gradient=p.gradient;
            	//p.last_direction=p.direction;
            	p.direction=-p.gradient;
            }
    }
    
};

class AverageGradientDirection extends DescentDirection
{
    int reinit=100000; //1000;
	HashMap<Parameter,Double> nbs=new HashMap<Parameter,Double>(); 
    HashMap<Parameter,Double> sums=new HashMap<Parameter,Double>();
    double ratio=0.5;
	public void computeDirection(MLPModel loss){
            nb++;
            Parameters params=loss.getUsedParams();
            for(Parameter p:params.getParams()){
            	Double n=nbs.get(p);
            	n=(n==null)?0:n;
            	Double s=sums.get(p);
            	s=(s==null)?0.0:s;
            	p.last_gradient=p.gradient;
            	s=ratio*s+p.gradient;
            	n=ratio*n+1.0;
            	if((reinit>0) && (n>reinit)){
            		n=1.0;
            		s=1.0*p.gradient;
            	}
            	sums.put(p, s);
            	nbs.put(p, n);
            	
            	double av=(s/n);
            	//p.last_direction=p.direction;
            	p.direction=(float)(-av);
            }
    }
    
};

class GradientSignDirection extends DescentDirection
{
    public void computeDirection(MLPModel loss){
            nb++;
            Parameters params=loss.getUsedParams();
            for(Parameter p:params.getParams()){
            	
            	
            	p.last_gradient=p.gradient;
            	//p.last_direction=p.direction;
            	if(p.gradient>0){
            		p.direction=-1.0f;
            	}
            	else{
            		if(p.gradient<0){
            			p.direction=1.0f;
            		}
            		else{
            			p.direction=0.0f;
            		}
            	}
            	//p.direction+=p.last_gradient;
            	
            }
    }
    
};


// Fletcher-Reeves
class FR_ConjugateGradientDirection extends DescentDirection
{
    protected int nb_reinit; // direction nulle tous les nb_reinit (si <=0 => jamais)
    public FR_ConjugateGradientDirection(){
    	this(0);
    }
    public FR_ConjugateGradientDirection(int _nb_reinit){
    	nb_reinit=_nb_reinit;
    }
    public void computeDirection(MLPModel loss){
    		Parameters params=loss.getParams();
            nb++;
            double norm=0.0;
            double normlast=0.0;
            for(Parameter p:params.getParams()){
            	//p.last_direction=p.direction;
            	if ((nb_reinit>0) && (nb % nb_reinit == 0)){
            		p.gradient=0;
                    p.last_gradient=p.gradient;
                    p.direction=0;
                    continue;
            	}
            	p.direction=-p.gradient;
            	norm+=p.gradient*p.gradient;
                normlast+=p.last_gradient*p.last_gradient;
            }
            double beta=norm;
            if (normlast!=0){
                beta/=normlast;
            }
            for(Parameter p:params.getParams()){
            	if ((nb_reinit>0) && (nb % nb_reinit == 0)){
            		continue;
            	}
            	p.direction+=beta*p.last_direction;
            	p.last_gradient=p.gradient;
                
            }
            
        }
}

// Polak-Ribiere
class PR_ConjugateGradientDirection extends DescentDirection
{
    protected int nb_reinit; // direction nulle tous les nb_reinit (si <=0 => jamais)
    protected boolean sup0; // si true, alors on prend beta=max[0,beta]
    public PR_ConjugateGradientDirection(){
    	this(true,0);
    }
    public PR_ConjugateGradientDirection(boolean _sup0, int _nb_reinit){
    	sup0=_sup0; nb_reinit=_nb_reinit;
    }
    public void computeDirection(MLPModel loss){
    		Parameters params=loss.getParams();
            nb++;
            double norm=0.0;
            double normlast=0.0;
            
            for(Parameter p:params.getParams()){
            	//p.last_direction=p.direction;
            	if ((nb_reinit>0) && (nb % nb_reinit == 0)){
            		p.gradient=0;
                    p.last_gradient=p.gradient;
                    p.direction=0;
                    continue;
            	}
            	p.direction=-p.gradient;
            	norm+=(p.gradient-p.last_gradient)*p.gradient;
                normlast+=p.last_gradient*p.last_gradient;
                
            }
      
            double beta=norm;
            if (normlast!=0){
                beta/=normlast;
            }
            if ((sup0) && (beta<0)){
               beta=0;
            }
            
            for(Parameter p:params.getParams()){
            	if ((nb_reinit>0) && (nb % nb_reinit == 0)){
            		continue;
            	}
            	p.direction+=beta*p.last_direction;
            	p.last_gradient=p.gradient;
                
            }
     }
   
}

//Benjamin (je ne sais pas le nom du truc, il faut lui demander)
class BenjaminGradientDirection extends DescentDirection
{
	protected double lambda;  
	protected int nb_reinit; // direction nulle tous les nb_reinit (si <=0 => jamais)
	HashMap<Parameter,Double> gis=new HashMap<Parameter,Double>();
	public BenjaminGradientDirection(){
		this(0.5,-1);
	}
	public BenjaminGradientDirection(double lambda){
		this(lambda,-1);
	}
	public BenjaminGradientDirection(double lambda, int _nb_reinit){
		this.lambda=lambda; nb_reinit=_nb_reinit;
	}
	public void computeDirection(MLPModel loss){
     nb++;
     Parameters params=loss.getUsedParams();
     for(Parameter p:params.getParams()){
     	Double g=gis.get(p);
     	g=(g==null)?0.0:g;
     	p.last_gradient=p.gradient;
     	g=lambda*g+(1.0-lambda)*p.gradient*p.gradient;
     	if((nb_reinit>0) && (nb%nb_reinit==0)){
     		g=1.0*p.gradient*p.gradient;
     	}
     	gis.put(p, g);
     	if(p.gradient>0){
     		p.direction=(float)(-Math.sqrt(g)/p.gradient);
     		if(p.direction<-100){
     			p.direction=-100;
     		}
     	}
     	else{
     		p.direction=0.0;
     	}
     }
	}

}