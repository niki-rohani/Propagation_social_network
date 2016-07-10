package experiments;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ResultFile {
	private String fileName;
	private ArrayList<String> cols;
	private HashMap<String,Integer> col_sizes;
	private ArrayList<HashMap<String,String>> lines;
	private String entete="";
	private boolean rewriteNeeded;
	//private ArrayList<Result> res;
	public ResultFile(String fileName){
		this.fileName=fileName;
		cols=new ArrayList<String>();
		lines=new ArrayList<HashMap<String,String>>();
		HashMap<String,String> name_line=new HashMap<String,String>();
		lines.add(name_line);
		name_line.put("Data", "Data ");
		col_sizes=new HashMap<String,Integer>();
		cols.add("Data");
		col_sizes.put("Data", 5);
		rewriteNeeded=false;
		//res=new ArrayList<Result>();
		entete="";
	}
	
	
	// if from<0 => write everything
	public void writeLastLines(int from)  throws IOException{
		File f=new File(fileName);
		PrintWriter out=null;
		try{
			if (from<0){
				 out=new PrintWriter(new BufferedWriter(new FileWriter(f)));
				 out.println(entete);
				 from=0;
			}
			else{
				out=new PrintWriter(new BufferedWriter(new FileWriter(f,true)));
			}
			while(from<lines.size()){
				String s="";
				HashMap<String,String> h=lines.get(from);
				for(String c:cols){
					String g=h.get(c);
					if (g==null){
						g="NA";
					}
					int lg=g.length();
					int sc=col_sizes.get(c);
					while(lg<sc){
						g+=" ";
						lg++;
					}
					s+=g+"\t\t";
				}
				//System.out.println(s);
				out.println(s);
				from++;
			}
			this.rewriteNeeded=false;
			
		}
		catch(IOException e){
			System.out.println(e);
			throw e;
		}
		finally{
			out.close();
		}
	}
	
	public void addResult(Result res){
		 ArrayList<String> score_names=res.score_names;
		 HashMap<String,String> name_line=lines.get(0);
		 HashMap<String,String> line=new HashMap<String,String>();
		 lines.add(line);
		 String d=res.getDonnee();
		 line.put("Data",d);
		 if (d.length()>col_sizes.get("Data")){
			 col_sizes.put("Data", d.length()+1);
			 this.rewriteNeeded=true;
		 }
		 DecimalFormat format = new DecimalFormat();
		 format.setMaximumFractionDigits(5);
		 format.setGroupingUsed(false);
		 for(String sc:score_names){
			 String sv="";
			 Double val=res.getScore(sc);
			 if (val==null){
				 sv="NA";
			 }
			 else{
				 sv=format.format(val);
			 }
			 if(!col_sizes.containsKey(sc)){
				 name_line.put(sc,sc);
				 col_sizes.put(sc, sc.length()+1);
				 cols.add(sc);
				 this.rewriteNeeded=true;
			 }
			 line.put(sc,sv);
			 if (sv.length()>col_sizes.get(sc)){
				 col_sizes.put(sc, sv.length()+1);
				 this.rewriteNeeded=true;
			 }
		 }
	}
	
	public void append(Result result) throws IOException{
		ArrayList<Result> results=new ArrayList<Result>();
		results.add(result);
		append(results);
	}
	public void append(ArrayList<Result> results) throws IOException{
		int nbl=lines.size();
		
		if ((nbl==1) && (entete.length()==0)){
			String s="Pour Expe : \n";
			s+=results.get(0).experiment;
			s+="\n";
			entete=s;
			this.rewriteNeeded=true;
		}
		
		for(Result res:results){
			 addResult(res);
		}
		if (this.rewriteNeeded){
			writeLastLines(-1);
		}
		else{
			writeLastLines(nbl);
		}
	}
	public void setEntete(String entete) throws IOException{
		this.entete=entete;
		this.rewriteNeeded=true;
		writeLastLines(-1);
	}
	
}
