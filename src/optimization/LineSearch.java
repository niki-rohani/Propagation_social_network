package optimization;

import java.util.HashMap;
import core.Environnement;
import core.Utils;
public abstract class LineSearch {
	public LineSearch() {}
    public abstract double getLine(Fonction fonc, HashMap<Integer, Double> direction, double epsilon);
    public void reinit(){}
    public void produceThis(double line){}
}

class LineSearch0 extends LineSearch
{
    protected double init_line; // valeur de line courante

    public LineSearch0(){
    	this(1.0);
    }
    public LineSearch0(double _init_line) {
            init_line=_init_line;
    }
    public double getLine(Fonction fonc, HashMap<Integer, Double> direction, double epsilon){
           return(init_line);
    }
        
}


class LineSearch1 extends LineSearch
{
    protected double init_line; // valeur de line courante
    protected double x; // nombre de fois que getLine a ete appele (sert pour le calcul de la prochaine Line)
    public LineSearch1() {
    	this(1.0);
    }
    public LineSearch1(double _init_line) {
            init_line=_init_line;
            x=0;
    }
    public double getLine(Fonction fonc, HashMap<Integer, Double> direction, double epsilon){
        	      double line=init_line/(1+x);
           x++;
           return(line);
    }
    public void reinit(){x=0;}
    
}

class LineSearch2 extends LineSearch
{
    protected double init_line; // valeur de line courante
    protected double x; // nombre de fois que getLine a ete appele (sert pour le calcul de la prochaine Line)
    public LineSearch2(){
    	this(1.0);
    }
    public LineSearch2(double _init_line) {
            init_line=_init_line;
            x=0;
    }
    public double getLine(Fonction fonc, HashMap<Integer, Double> direction, double epsilon){
           double line=init_line/(init_line+x);
           x++;
           return(line);
     }
     public void reinit(){x=0;}   
}

class LineSearch3 extends LineSearch
{
    protected double x; // nombre de fois que getLine a ete appele (sert pour le calcul de la prochaine Line)
    protected double line; // valeur de line courante
    public LineSearch3(){
    	this(1.0);
    }
    public LineSearch3(double _init_line) {
            line=_init_line;
            x=0;
    }
    public double getLine(Fonction fonc, HashMap<Integer, Double> direction, double epsilon){
            line=line/(line+1);
            x++;
            return(line);
    }
    public void reinit(){x=0;}
}

class LineSearch4 extends LineSearch
{
    protected double init_line; // valeur de line courante
    protected double x; // nombre de fois que getLine a ete appele (sert pour le calcul de la prochaine Line)
    protected double step;
    public LineSearch4(){
    	this(1.0,1.0);
    }
    public LineSearch4(double _init_line,double _step) {
            init_line=_init_line;
            x=0;
            step=_step;
    }
    public double getLine(Fonction fonc, HashMap<Integer, Double> direction, double epsilon){
            double line=init_line/(1+(x*step));
            x++;
            return(line);
    }
    public void reinit(){x=0;}
};

class LineSearchDicho extends LineSearch
{
    protected double init_line;
    protected double line; // valeur de line courante
    protected double factor; // facteur par lequel  multiplier si line est trop petit
    protected double min_inter; // dicho jusqu a intervalle entre min et sup < min_inter
    public LineSearchDicho(){
    	this(0.00001,2.0,0.00000000001);
    }
    public LineSearchDicho(double _init_line, double _factor, double _min_inter) {
            init_line=_init_line;
            line=_init_line;
            factor=_factor;
            min_inter=_min_inter;
     }
    public double getLine(Fonction fonc, HashMap<Integer, Double> direction, double epsilon){
            double ret=line;
            int verbose=Environnement.getVerbose();
            Parameters pars=fonc.getParams();
            HashMap<Integer,Double> params=pars.getParams(); //->copy();
             //min_inter=line/10.0;
             int nb=0;
             double last_val;
             double val=fonc.getValue();
             double old_val=val;
             double min_val=val;
             boolean godown=false;
             double line_min=0;
             boolean ok=false;
             HashMap<Integer,Double> ngrad;
             double inf=0;
             double sup=-1;
             double last_line=ret;
             double val_sup=old_val;
             double val_inf;
             int nb_sans_best=0;


             do{

                pars.add(direction,ret);
                last_val=val;
                val=fonc.getValue();
                ngrad=fonc.getGradient();
                nb_sans_best++;
                if (val<min_val){
                    min_val=val;
                    line_min=ret;
                    nb_sans_best=0;
                    //ok=true;
                }
                double dot=Utils.computeDot(direction,ngrad);
                godown=true;
                if (dot<0){
                    godown=false;
                }
                
                if (verbose>=3){
                	  System.out.println("          Recherche Line "+nb+" entre "+inf+" et "+sup);
                      System.out.println("          Value : "+val);
                      System.out.println("          Old Value : "+old_val);
                      System.out.println("          Params : "+pars);
                      System.out.println("          pas : "+ret);
                      System.out.println("          Gradient : "+ngrad);
                      System.out.println("          Min Line : "+line_min);
                      System.out.println("          Direction * next Gradient : "+dot+"=> godown = "+godown);
                }
                pars.add(direction,-1.0*ret);
                last_line=ret;
                if (godown){
                    sup=ret;
                    val_sup=val;
                    ret=(inf+sup)/2;
                    ok=true;
                }
                else{
                    inf=ret;
                    val_inf=val;
                    if (inf>sup){
                       ret=ret*factor;
                    }
                    else{
                        ret=(inf+sup)/2;
                    }

                }
                nb++;

             //}while((!ok) || (std::abs(val_inf-val_sup)>epsilon));
             //}while((line_min==0) || (nb_sans_best<10));
             //}while(std::abs(sup-inf)>0);
             }while(((Math.abs(sup-inf)>=min_inter) || (line_min==0)) && (nb<100));
             //while((ok) || ((line_min==0) && (ret!=0))); //(nb<100000)));
             //}while(std::abs(val-last_val)>epsilon);
             //}while(((val*line)<(old_val*line)) && (std::abs(val-last_val)>epsilon));   // Le fait de multiplier par line permet de gerer le cas de maximization comme la minimization

              if (line_min!=0){
                line=line_min;
              }


              return(line_min);
        }
        public void reinit(){line=init_line;}


}

class LineSearch5 extends LineSearch
{
    protected double init_line; // valeur de line courante
    protected double x; // nombre de fois que getLine a ete appele (sert pour le calcul de la prochaine Line)
    protected double step;
    protected int nb;
    public LineSearch5(){
    	this(1.0,1.0);
    }
    public LineSearch5(double _init_line,double _step) {
            init_line=_init_line;
            x=0;
            nb=0;
            step=_step;
    }
    public double getLine(Fonction fonc, HashMap<Integer, Double> direction, double epsilon){
           double line=init_line/(1+(x*step));

           if (nb % 1000==0){
              LineSearch ld=new LineSearchDicho();
              line=ld.getLine(fonc,direction,epsilon);
              //line=init_line;
              x=(init_line-line)/(line*step);
           }
           x+=1.0;
           nb++;
           //SPTR<Vector> params=(fonc->getParams())->copy();
           //params->add(gradient,line);
           //fonc->setParams(params);
           return(line);
    }
    public void reinit(){x=0;}
}

class LineSearch6 extends LineSearch
{
    protected double init_line; // valeur de line courante
    protected double x; // nombre de fois que getLine a ete appele (sert pour le calcul de la prochaine Line)
    protected double step;
    protected int nb;
    public LineSearch6(){
    	this(1.0,1.0);
    }
    public LineSearch6(double _init_line,double _step) {
            init_line=_init_line;
            x=0;
            nb=0;
            step=_step;
    }
    public double getLine(Fonction fonc, HashMap<Integer, Double> direction, double epsilon){
          double line=init_line/(1+(x*step));

           if (nb % 100==0){
              LineSearch ld=new LineSearchDicho();
              line=ld.getLine(fonc,direction,epsilon);
              //line=init_line;
              x=(init_line-line)/(line*step);
           }
           x+=1.0;
           nb++;
           //SPTR<Vector> params=(fonc->getParams())->copy();
           //params->add(gradient,line);
           //fonc->setParams(params);
           return(line);
        }
        public void reinit(){x=0;}
        public void produceThis(double _line){
           x=(init_line-_line)/(_line*step);
           x+=1.0;
        }
 
}
