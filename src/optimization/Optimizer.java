package optimization;
import java.util.HashMap;

import core.Environnement;
import core.Utils;
import java.io.File;
public abstract class Optimizer {
	
	public void optimize(Fonction fonc){
   	 optimize(fonc,0.0000000001,1000,10);
    }
    public void optimize(Fonction fonc, double epsilon){
   	 optimize(fonc,epsilon,1000,10);
    }
    public void optimize(Fonction fonc, double epsilon, int maxit){
   	 optimize(fonc,epsilon,maxit,10);
    }
	
	public void optimize(Fonction fonc, double epsilon, int maxit,int minit){
		optimize(fonc,epsilon,maxit,minit,-1);
	}
	public abstract void optimize(Fonction fonc, double epsilon, int maxit,int minit,double max_loss);
}


class Descent extends Optimizer
{
     protected LineSearch linesearch;
     protected DescentDirection descdir;
     protected boolean normalize_grad;
     
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
     
     public void optimize(Fonction fonc, double epsilon, int maxit, int minit, double max_loss){
             int verbose=Environnement.getVerbose();

             LineSearch alt=new LineSearchDicho();
             LineSearch xx=new LineSearch6();

             descdir.setFonction(fonc);
             Parameters params=fonc.getParams(); //.getParams(); //->copy();

             HashMap<Integer,Double> gradient=new  HashMap<Integer,Double>();
             HashMap<Integer,Double> direction=new  HashMap<Integer,Double>();
             double line=0;
             int nb=0;
             //double epsilon=fonc->getEpsilon();
             double value=fonc.getValue();
             double oldval=value;
             double diff=0;
             double minval=value;
             HashMap<Integer,Double> pminval=new HashMap<Integer,Double>(params.getParams());
             gradient=fonc.getGradient();
             direction=descdir.getDirection();
             //double norm_grad=gradient->normSquare();
             //double old_norm_grad=norm_grad;
             
             //maxit=10000;
             int nb100=100;
             boolean stop_opt=false;
             do{
                //old_norm_grad=norm_grad;
                if (normalize_grad){
                   double norm_dir=Utils.getNormSquared(direction);
                   if (norm_dir!=0){
                     direction=Utils.multiplies(direction,1.0/Math.sqrt(norm_dir));
                   }
                   //double norm_dir2=direction->normSquare();
                   //System.out.println("norms "<<norm_dir<<","<<norm_dir2<<"\n";
                }
                if (verbose>=2){
                	if (nb%100==0){
                      System.out.println("Passe "+nb);
                      System.out.println("Value : "+value);
                      System.out.println("pas applique : "+line);
                      System.out.println("gain : "+diff);
                    
                      nb100=0;
                      System.out.println("Params : "+fonc.getParams());
                      System.out.println("Nouveau gradient : "+gradient);
                      System.out.println("Nouvelle direction : "+direction+"\n");    
                    }
                }
                File file=new File("stop.txt");
                if (nb%100==0){
                	if (file.exists()){
                  	  stop_opt=true;
                    }
                }

                line=linesearch.getLine(fonc,direction,epsilon);
                params.add(direction,line);
                oldval=value;
                value=fonc.getValue();
                diff=value-oldval;

                if ((linesearch.getClass()== xx.getClass()) && (diff>0)){
                	 if (verbose>=2){
                		 System.out.println("Passe "+(nb+1));
                		 System.out.println("Value : "+value);
                		 System.out.println("pas applique : "+line);
                		 System.out.println("gain positif : "+diff);
                	 }
                	params.add(direction,-1.0*line);
                    //fonc->setParams(params);
                    line=alt.getLine(fonc,direction,epsilon);
                    params.add(direction,line);
                    //fonc->setParams(params);
                    value=fonc.getValue();
                    diff=value-oldval;
                    linesearch.produceThis(line);
                }

                if (value<minval){
                	minval=value;
                	pminval=new HashMap<Integer,Double>(params.getParams());
                }
                gradient=fonc.getGradient();
                direction=descdir.getDirection();
                //norm_grad=gradient->normSquare();
                //nbViolatedConstraints=fonc->getNbViolatedConstraints();

                nb++;
                nb100++;

             }while((nb<maxit) && (line>0) && (((max_loss>=0) && (value>max_loss)) || (Math.abs(diff)>epsilon) || (nb<minit)) && (!stop_opt)); // && (norm_grad>0.0000001)); //((std::abs(diff)>epsilon) || (norm_grad<old_norm_grad) || (old_nb_viol<nbViolatedConstraints)));
             if (minval<value){
            	 System.out.println("restore min val");
            	 params.setParams(pminval);
            	 value=fonc.getValue();
            	 gradient=fonc.getGradient();
                 direction=descdir.getDirection();
             }
             
             if (verbose>=1){
                      System.out.println("Last Passe ("+nb+" passes)");
                      System.out.println("Value : "+value);
                      System.out.println("Params : "+fonc.getParams().toString());
                      System.out.println("pas applique : "+line);
                      System.out.println("gain : "+diff);
                      System.out.println("Nouveau gradient : "+gradient);
                      System.out.println("Nouvelle direction : "+direction+"\n");
             }
             linesearch.reinit();
        }

};