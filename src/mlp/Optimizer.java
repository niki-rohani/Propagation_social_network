package mlp;

import java.util.HashMap;

//import core.Environnement;
//import core.Utils;
import java.io.File;
public abstract class Optimizer {
	
	
	public void optimize(MLPModel loss){
		optimize(loss,false);
	}
	public void optimize(MLPModel loss, boolean copyParams){
   	 optimize(loss,0.0,10000000000l,10,copyParams);
    }
    public void optimize(MLPModel loss, double epsilon, boolean copyParams){
   	 optimize(loss,epsilon,10000000000l,10,copyParams);
    }
    public void optimize(MLPModel loss, double epsilon, long maxit, boolean copyParams){
   	 optimize(loss,epsilon,maxit,10,copyParams);
    }
	
	public void optimize(MLPModel loss, double epsilon, long maxit,int minit, boolean copyParams){
		optimize(loss,epsilon,maxit,minit,-1,copyParams);
	}
	public abstract void optimize(MLPModel loss, double epsilon, long maxit,int minit,double max_loss,boolean copyParams);
	
	public static Descent getDescent(DescentDirection dir, LineSearch line){
		return new Descent(dir,line);
	}
}


class Descent extends Optimizer
{
     protected LineSearch linesearch;
     protected DescentDirection descdir;
     protected boolean normalize_grad; // pas utilise
     
     public Descent(){
    	 this(new GradientDirection(),new LineSearchDicho(),false);
     }
     public Descent(DescentDirection dir){
    	 this(dir,new LineSearchDicho(),false);
     }
     public Descent(DescentDirection dir,LineSearch line){
    	 this(dir,line,false);
     }
     public Descent(DescentDirection dir, LineSearch line,boolean normalize_grad){
            this.linesearch=line;
            this.descdir=dir;
            this.normalize_grad=normalize_grad;
     }
     
     public void optimize(MLPModel loss, double epsilon, long maxit, int minit, double max_loss,boolean copyParams){
             int verbose=Env.getVerbose();

             LineSearch alt=new LineSearchDicho();
             LineSearch xx=new LineSearch6();

             //descdir.setFonction(fonc);
             
             
             Parameters params=loss.getParams(); //.getParams(); //->copy();
             /*params.update(0.0f);*/
             double line=0;
             long nb=0l;
             //double epsilon=fonc->getEpsilon();
             loss.forward();
             
             double value=loss.getLossValue();
             double oldval=value;
             double diff=0;
             double minval=value;
             //params.computeGradient(loss);
             loss.backward();
            
             if(copyParams){
            	 loss.copyParams();
             }
             //Parameters pminval=new Parameters(params);
             descdir.computeDirection(loss);
             //System.out.println(params);
             //double norm_grad=gradient->normSquare();
             //double old_norm_grad=norm_grad;
             
             //maxit=10000;
             int nb100=100;
             boolean stop_opt=false;
             do{
                //old_norm_grad=norm_grad;
                /*if (normalize_grad){
                   double norm_dir=Utils.getNormSquared(direction);
                   if (norm_dir!=0){
                     direction=Utils.multiplies(direction,1.0/Math.sqrt(norm_dir));
                   }
                   //double norm_dir2=direction->normSquare();
                   //System.out.println("norms "<<norm_dir<<","<<norm_dir2<<"\n";
                }*/
                if (verbose>=1){
                	if (nb%1==0){
                      System.out.println("Passe "+nb);
                      System.out.println("Value : "+value);
                      System.out.println("pas applique : "+line);
                      System.out.println("gain : "+diff);
                    
                      nb100=0;
                      if(verbose>=3){
                    	  System.out.println("Params : "+params);
                      }
                      //System.out.println("Nouveau gradient : "+gradient);
                      //System.out.println("Nouvelle direction : "+direction+"\n");    
                    }
                }
                File file=new File("stop.txt");
                if ((verbose>=1) && (nb%100==0)){
                	if (file.exists()){
                  	  stop_opt=true;
                    }
                }
                

                line=linesearch.getLine(loss,epsilon);
                if(nb%1000==0){
                	System.out.println("line = "+line);
                }
                loss.updateParams((float)line);
                
                oldval=value;
                loss.forward();
                value=loss.getLossValue();
                diff=value-oldval;

                if ((linesearch.getClass()== xx.getClass()) && (diff>0)){
                	 if (verbose>=2){
                		 System.out.println("Passe "+(nb+1));
                		 System.out.println("Value : "+value);
                		 System.out.println("pas applique : "+line);
                		 System.out.println("gain positif : "+diff);
                	 }
                	loss.revertLastMove(); //.updateParams((float)(-1.0*line));
                    //fonc->setParams(params);
                    line=alt.getLine(loss,epsilon);
                    loss.updateParams((float)line);
                    //fonc->setParams(params);
                    loss.forward();
                    value=loss.getLossValue();
                    diff=value-oldval;
                    linesearch.produceThis(line);
                }

                if (value<minval){
                	minval=value;
                	if(copyParams){
                   	 loss.copyParams();
                    }
                	
                }
                loss.backward();
                descdir.computeDirection(loss);
                //norm_grad=gradient->normSquare();
                //nbViolatedConstraints=fonc->getNbViolatedConstraints();

                nb++;
                nb100++;
                if(nb>maxit){
                	stop_opt=true;
                }
                
                if((Math.abs(diff)<epsilon) && (nb>=minit)){
                	stop_opt=true;
                }
                
             //}while(!(new File("stop")).exists());
             //}while((nb<maxit) && (line>0) && (((max_loss>=0) && (value>max_loss)) || (Math.abs(diff)>epsilon) || (nb<minit)) && (!stop_opt)); // && (norm_grad>0.0000001)); //((std::abs(diff)>epsilon) || (norm_grad<old_norm_grad) || (old_nb_viol<nbViolatedConstraints)));
     		 //}while((nb<maxit) && (line>0) && (((max_loss>=0) && (value>max_loss)) ||  (nb<minit)) && (!stop_opt)); // && (norm_grad>0.0000001)); //((std::abs(diff)>epsilon) || (norm_grad<old_norm_grad) || (old_nb_viol<nbViolatedConstraints)));
             }while(!stop_opt); 
                
             /*
             if (minval<value){
            	 System.out.println("restore min val");
            	 //params.updateParams(pminval);
            	 
            	 if(copyParams){
                	 loss.loadCopy();
                 }
            	 loss.forward();
            	 value=loss.getLossValue();
            	 loss.backward();
                 descdir.computeDirection(loss);
             }*/
             
             if (verbose>=1){
                      System.out.println("Last Passe ("+nb+" passes)");
                      System.out.println("Value : "+value);
                      //System.out.println("Params : "+params);
                      System.out.println("pas applique : "+line);
                      System.out.println("gain : "+diff);
                      //System.out.println("Nouveau gradient : "+gradient);
                      //System.out.println("Nouvelle direction : "+direction+"\n");
             }
             linesearch.reinit();
        }
     
     	

};
