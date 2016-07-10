package optimization;

import java.util.HashMap;
import java.util.ArrayList;
import core.Utils;

public abstract class DescentDirection {
	protected Fonction fonc;
    protected HashMap<Integer,Double> lastDirection;
    protected HashMap<Integer,Double> lastGradient;
    protected int nb; // nombre d'appels de getDirection

    public DescentDirection() {
            fonc=null;
            lastDirection=new HashMap<Integer,Double>();
            lastGradient=new HashMap<Integer,Double>();
            nb=0;
     }
     public abstract HashMap<Integer,Double> getDirection();
     public void setFonction(Fonction _fonc){
            fonc=_fonc;
            lastDirection=new HashMap<Integer,Double>();
            lastGradient=new HashMap<Integer,Double>();
     }
     public void reset(){
        	lastDirection=new HashMap<Integer,Double>();
            lastGradient=new HashMap<Integer,Double>();
     }
    
}

class GradientDirection extends DescentDirection
{
    public HashMap<Integer,Double> getDirection(){
            nb++;
            HashMap<Integer,Double> gradient=fonc.getGradient();
            lastGradient=gradient;
            HashMap<Integer,Double> direction=Utils.multiplies(gradient,-1.0);
            lastDirection=direction;
            return(direction);
    }
    
};


// Fletcher-Reeves
class FR_ConjugateGradientDirection extends DescentDirection
{
    protected int nb_reinit; // appel a reset tous les nb_reinit (si <=0 => jamais)
    public FR_ConjugateGradientDirection(){
    	this(0);
    }
    public FR_ConjugateGradientDirection(int _nb_reinit){
    	nb_reinit=_nb_reinit;
    }
    public HashMap<Integer,Double> getDirection(){
            nb++;
            if ((nb_reinit>0) && (nb % nb_reinit == 0)){reset();}

            HashMap<Integer,Double> gradient=fonc.getGradient();
            HashMap<Integer,Double> direction=Utils.multiplies(gradient,-1.0);
            

            double norm=Utils.getNormSquared(gradient);
            double normlast=Utils.getNormSquared(lastGradient);

            double beta=norm;
            if (normlast!=0){
                beta/=normlast;
            }
            direction=Utils.add(direction,lastDirection,beta);

            lastGradient=Utils.copy(gradient);
            lastDirection=Utils.copy(direction);
            return(direction);
        }
}

// Polak-Ribiere
class PR_ConjugateGradientDirection extends DescentDirection
{
    protected int nb_reinit; // appel a reset tous les nb_reinit (si <=0 => jamais)
    protected boolean sup0; // si true, alors on prend beta=max[0,beta]
    public PR_ConjugateGradientDirection(){
    	this(true,0);
    }
    public PR_ConjugateGradientDirection(boolean _sup0, int _nb_reinit){
    	sup0=_sup0; nb_reinit=_nb_reinit;
    }
    public HashMap<Integer,Double> getDirection(){
            nb++;
            if ((nb_reinit>0) && (nb % nb_reinit == 0)){reset();}

            HashMap<Integer,Double> gradient=fonc.getGradient();
            HashMap<Integer,Double> direction=Utils.multiplies(gradient,-1.0);
            

            HashMap<Integer,Double> gg=Utils.add(gradient,lastGradient,-1.0);
            double norm=Utils.computeDot(gradient,gg);
            double normlast=Utils.getNormSquared(lastGradient);

            double beta=norm;
            if (normlast!=0){
                beta/=normlast;
            }
            if ((sup0) && (beta<0)){
               beta=0;
            }
            direction=Utils.add(direction,lastDirection,beta);

            lastGradient=Utils.copy(gradient);
            lastDirection=Utils.copy(direction);
            return(direction);
        }
   
}