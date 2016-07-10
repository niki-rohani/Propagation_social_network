package propagationModels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeMap;

import utils.ValInHashMapComparator;
import actionsBD.MongoDB;
import cascades.Cascade;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;
import core.Text;
import core.User;

public class MultiSetsPropagationStructLoader extends PropagationStructLoader {
	
	private ArrayList<PropagationStructLoader> ploaders;
	/*public MultiSetsPropagationStructLoader(ArrayList<String> db,ArrayList<String> collection,ArrayList<Long> step){
   	 this(db,collection,step,null,null);
    }*/
    
	public MultiSetsPropagationStructLoader(String db,String collection,long step,String ratioInits,String nbMaxInits){
    	this(db.split(","),collection.split(","),step,ratioInits.split(","),nbMaxInits.split(","),null,null);
    }
	
    public MultiSetsPropagationStructLoader(String[] db,String[] collection,long step,String[] ratioInits,String[] nbMaxInits){
   	 this(db,collection,step,ratioInits,nbMaxInits,null,null);
    }
   
    public MultiSetsPropagationStructLoader(String db,String collection,long step,String ratioInits,String nbMaxInits, String start, String nbMax){
    	this(db.split(","),collection.split(","),step,ratioInits.split(","),nbMaxInits.split(","),start.split(","),nbMax.split(","));
    }
    
    public MultiSetsPropagationStructLoader(String[] db,String[] collection,long step,String[] ratioInits,String[] nbMaxInits, String[] start, String[] nbMax){
   	 super("","",step,1.0,-1,0,-1);
   	 int nbCols=collection.length;
   	 if(nbCols==0){
   		 throw new RuntimeException("No Db Given !");
   	 }
   	 if(ratioInits!=null){
   		 if(ratioInits.length==1){
   			String v=ratioInits[0];
   			ratioInits=new String[nbCols];
   			for(int i=0;i<nbCols;i++){
   				ratioInits[i]=v;
   			}
   		 }
   	}
   	if(nbMaxInits!=null){
  		 if(nbMaxInits.length==1){
  			String v=nbMaxInits[0];
  			nbMaxInits=new String[nbCols];
  			for(int i=0;i<nbCols;i++){
  				nbMaxInits[i]=v;
  			}
  		 }
  	}
   	if(start!=null){
  		 if(start.length==1){
  			String v=start[0];
  			start=new String[nbCols];
  			for(int i=0;i<nbCols;i++){
  				start[i]=v;
  			}
  		 }
  	 }
   	 if(nbMax!=null){
  		 if(nbMax.length==1){
  			String v=nbMax[0];
  			nbMax=new String[nbCols];
  			for(int i=0;i<nbCols;i++){
  				nbMax[i]=v;
  			}
  		 }
  	 }
   	 if(db.length==1){
  			String v=db[0];
  			db=new String[nbCols];
  			for(int i=0;i<nbCols;i++){
  				db[i]=v;
  			}
  	 }
  	 
   	 ploaders=new ArrayList<PropagationStructLoader>();
     for(int i=0;i<nbCols;i++){
    	 String idb=db[i];
    	 String icollection=collection[i];
    	 
    	 Double iratioInits=1.0;
    	 if(ratioInits!=null){
    		 iratioInits=Double.valueOf(ratioInits[i]);
    	 }
    	 Integer inbMaxInits=-1;
    	 if(nbMaxInits!=null){
    		 inbMaxInits=Integer.parseInt(nbMaxInits[i]);
    	 }
    	 Integer istart=0;
    	 if(start!=null){
    		 istart=Integer.parseInt(start[i]);
    	 }
    	 Integer inbMax=-1;
    	 if(nbMax!=null){
    		 inbMax=Integer.parseInt(nbMax[i]);
    	 }
    	 if(idb.length()==0){
    		 throw new RuntimeException("Empty Db!");
    	 }
    	 if(icollection.length()==0){
    		 throw new RuntimeException("Empty Collection!");
    	 }
    	 
    	 PropagationStructLoader prop=new PropagationStructLoader(idb,icollection,step,iratioInits,inbMaxInits,istart,inbMax);
    	 ploaders.add(prop);
     }
   	
    }

   
    
    
    @Override
	public int load(){
    	return load(0);
    }
    
    	@Override
	public int load(int minId){
    	int maxID=minId;
		for(PropagationStructLoader p:ploaders){
			if(this.computeFirsts){
				p.computeFirsts=true;
			}
			maxID=p.load(maxID)+1;
		}
		users_profiles=new HashMap<String,HashMap<Integer,Double>>();
        users_cascades=new HashMap<String,HashMap<Integer,Long>>();
        userAsFirst=new HashMap<String,ArrayList<Integer>>();
        cascades=new HashMap<Integer,PropagationStruct>();
        for(PropagationStructLoader p:ploaders){
        	HashMap<String,HashMap<Integer,Double>> up=p.getUsers_profiles();
        	HashMap<String,HashMap<Integer,Long>> uc=p.getUsers_cascades();
        	HashMap<String, ArrayList<Integer>> uf=p.getUserAsFirst();
        	HashMap<Integer,PropagationStruct> c=p.getCascades();
        	for(String u:up.keySet()){
        	   HashMap<Integer,Double> w=users_profiles.get(u);
          	   if(w==null){
          		   w=new HashMap<Integer,Double>(); 
          		  users_profiles.put(u, w);
          	   }
          	   HashMap<Integer,Double> wu=up.get(u);
          	   for(Integer i:wu.keySet()){
          		 double v=wu.get(i);
          		 Double o=w.get(i);
          		 o=(o==null)?0.0:o;
          		 w.put(i, o+v);
          	   }
        	}
        	for(String us:uc.keySet()){
        		 HashMap<Integer,Long> ucc=uc.get(us);
        		 HashMap<Integer,Long> usc=users_cascades.get(us);
        		 if(usc==null){
           		   usc=new HashMap<Integer,Long>();
           		   users_cascades.put(us,usc);
           	   	 }
        		 for(Integer ic:ucc.keySet()){
           	   	 	usc.put(ic,ucc.get(ic));
        		 }
        	}
        	for(Integer ic:c.keySet()){
        		cascades.put(ic, c.get(ic));
        	}
        	if(computeFirsts){
                for(String u:uf.keySet()){
                	ArrayList<Integer> a=userAsFirst.get(u);
                	if(a==null){
                		a=new ArrayList<Integer>();
                		userAsFirst.put(u,a);
                	}
                	a.addAll(uf.get(u));
                }
            }
        }
   	 	loaded=true;
   	 	return maxID;
    }

	@Override
	public String getCollection() {
		StringBuilder sb=new StringBuilder();
		int i=0;
		for(PropagationStructLoader p:ploaders){
			sb.append(p.getCollection());
			i++;
			if(i<ploaders.size()){
				sb.append(",");
			}
		}
		return sb.toString();
	}

	@Override
	public String getDb() {
		StringBuilder sb=new StringBuilder();
		int i=0;
		for(PropagationStructLoader p:ploaders){
			sb.append(p.getDb());
			i++;
			if(i<ploaders.size()){
				sb.append(",");
			}
		}
		return sb.toString();
	}

	@Override
	public String getStep() {
		/*StringBuilder sb=new StringBuilder();
		int i=0;
		for(PropagationStructLoader p:ploaders){
			sb.append(p.getStep());
			i++;
			if(i<ploaders.size()){
				sb.append(",");
			}
		}
		return sb.toString();*/
		return step+"";
	}

	@Override
	public String getRatioInits() {
		StringBuilder sb=new StringBuilder();
		int i=0;
		for(PropagationStructLoader p:ploaders){
			sb.append(p.getRatioInits());
			i++;
			if(i<ploaders.size()){
				sb.append(",");
			}
		}
		return sb.toString();
	}

	

	@Override
	public String getNbMaxInits() {
		StringBuilder sb=new StringBuilder();
		int i=0;
		for(PropagationStructLoader p:ploaders){
			sb.append(p.getNbMaxInits());
			i++;
			if(i<ploaders.size()){
				sb.append(",");
			}
		}
		return sb.toString();
	}
	
	public String getStart(){
		StringBuilder sb=new StringBuilder();
		int i=0;
		for(PropagationStructLoader p:ploaders){
			sb.append(p.getStart());
			i++;
			if(i<ploaders.size()){
				sb.append(",");
			}
		}
		return sb.toString();
	}
	
	public String getNbC(){
		StringBuilder sb=new StringBuilder();
		int i=0;
		for(PropagationStructLoader p:ploaders){
			sb.append(p.getNbC());
			i++;
			if(i<ploaders.size()){
				sb.append(",");
			}
		}
		return sb.toString();
	}
	
	public static void main(String[] args){
		MultiSetsPropagationStructLoader loader=new MultiSetsPropagationStructLoader(args[0],args[1],1,"1.0","-1","1","-1");
		loader.computeFirsts=true;
		loader.load(); 
		if(loader.computeFirsts){
       	 HashMap<String,Integer> nbFirsts=new HashMap<String,Integer>(); 
       	 for(String u:loader.getUserAsFirst().keySet()){
       		 nbFirsts.put(u, loader.userAsFirst.get(u).size());
       		 
       	 }
       	 ArrayList<String> lu=new ArrayList<String>(nbFirsts.keySet());
       	 Collections.sort(lu,new ValInHashMapComparator<String,Integer>(nbFirsts,true));
       	 for(String u:lu){
       		 System.out.println(u+" : "+loader.userAsFirst.get(u).size()+" cascades in First Step");
       	 }
        }
	}
}
