package utils;



import java.io.*;
import java.util.concurrent.TimeoutException;

public class ExecProc {
   protected String[] params;
   protected String output;
   protected String error;
   protected boolean ok_output;
   protected boolean ok_error;
   protected long timeout;
   protected int exit;
   public ExecProc(String[] args){
	   this(args,-1);
   }
   public ExecProc(String[] args,long timeout){
	params=args;
	output="";
	error="";
	ok_output=false;
	ok_error=false;
	this.timeout=timeout;
	exit=-1;
   }
   public int executeCommandLine(boolean write_output,boolean save_output)  throws IOException, InterruptedException, TimeoutException{
	Runtime runtime = Runtime.getRuntime();
	Worker worker=null;
	final Process process;
	Process p2=null;
	try{
		process  = runtime.exec(params);
		p2=process;
		final boolean sout=save_output;
		final boolean wout=write_output;
		// Consommation de la sortie standard de l'application externe dans un Thread separe
	//	if (write_output){
		new Thread() {
			public void run() {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					String line = "";
					try {
						while((line = reader.readLine()) != null) {
							// Traitement du flux de sortie de l'application si besoin est
							if (wout){
							    System.out.println(line);
							}
							if (sout){
							        output+=line+"\n";
							}
						}
					} finally {
						reader.close();
					}
				} catch(IOException ioe) {
					ioe.printStackTrace();
				}
				ok_output=true;
			}
		}.start();
		//}
	
		// Consommation de la sortie d'erreur de l'application externe dans un Thread separe
		new Thread(){
			public void run() {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					String line = "";
					try {
						while((line = reader.readLine()) != null) {
							// Traitement du flux d'erreur de l'application si besoin est
							System.out.println(line);
							
							     error+=line+"\n";
							
						}
					} finally {
						reader.close();
					}
				} catch(IOException ioe) {
					ioe.printStackTrace();
				}
				ok_error=true;
			}
		}.start();
	  if(timeout>0){
		worker = new Worker(process);
		worker.start();
		worker.join(timeout);
		if (worker.exit != null){exit = worker.exit;} else {throw new TimeoutException();}
	  }
	  else{
		  exit = process.waitFor(); 
	  }
     }catch(IOException e){System.out.println(e.getMessage());} 
	  catch(InterruptedException ex) {
		    worker.interrupt();
		    //Thread.currentThread().interrupt();
		    throw ex;
     } finally {
		    p2.destroy();
	 }
	return exit;
	
  }

/*		process.waitFor();
	}
	catch(IOException e){System.out.println(e.getMessage());}
	catch(InterruptedException e2){System.out.println(e2.getMessage());}
  }*/
		
	
		
		
  public String getOutput(){
	  return(output);
  }
  public String getError(){
	  return(error);
  }
  private static class Worker extends Thread {
		private final Process process;
		private Integer exit;
		private Worker(Process process) {
			this.process = process;
		}
		public void run() {
			try { 
				exit = process.waitFor();
			} catch (InterruptedException ignore) {
			return;
		}
	}
  }
}


/*
Worker worker = new Worker(process);
worker.start();
try {
  worker.join(timeout);
  if (worker.exit != null)
    return worker.exit;
  else
    throw new TimeoutException();
} catch(InterruptedException ex) {
  worker.interrupt();
  Thread.currentThread().interrupt();
  throw ex;
} finally {
  process.destroy();
}
}*/


