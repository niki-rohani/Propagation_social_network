package utils;
import java.util.HashMap;
import java.io.*;
public class Log {
	public static HashMap<String,Log> logs=new HashMap<String,Log>();
	public static int maxSize=3;
	public Writer log;
	public String file;
	//public String s="";
	
	// if file="", log in a string variable log of maximal length maxSize 
	private Log(String file) throws IOException{
		if(file.length()>0){
			File f=new File(file);
			File r=f.getParentFile();
			r.mkdirs();
			log=new PrintWriter(new BufferedWriter(new FileWriter(f)));
		}
		else{
			log=new StringWriter();
		}
		this.file=file;
		logs.put(file, this);
	}
	public static Log getLog(String file){
		Log l=logs.get(file);
		if(l==null){
			try{
				l=new Log(file);
			}
			catch(IOException e){
				throw new RuntimeException("Probleme logfile : "+e);
			}
		}
		return l;
	}
	public void print(String st){
		if(file.length()==0){
			((StringWriter)log).write(st);
			String s=log.toString();
			int l=s.length();
			if(l>maxSize){
				s=s.substring(l-maxSize);
				log=new StringWriter();
				((StringWriter)log).write(s);
			}
		}
		else{
			((PrintWriter)log).print(st);
			try{
				log.flush();
			}catch(IOException e){
				System.out.println("Flush log file pb");
			}
			//System.out.println(st);
		}
	}
	public void println(String st){
		if(file.length()==0){
			((StringWriter)log).write(st+"\n");
			String s=log.toString();
			int l=s.length();
			if(l>maxSize){
				s=s.substring(l-maxSize);
				log=new StringWriter();
				((StringWriter)log).write(s);
			}
		}
		else{
			((PrintWriter)log).println(st);
			try{
				log.flush();
			}catch(IOException e){
				System.out.println("Flush log file pb");
			}
		}
	}
	public String toString(){
		if(file.length()==0){
			return(log.toString());
		}
		else{
			return("Log "+file);
		}
	}
	public void finalize(){
		if(file.length()>0){
			try{
				log.flush();
			}catch(IOException e){
				System.out.println("Flush log file pb");
			}
			((PrintWriter)log).close();
		}
	}
	public static void main(String[] args){
		Log l=Log.getLog("./jkj/lm.txt");
		l.println("ee");
		l.println("xxxx");
		System.out.println(l);
	}
}


