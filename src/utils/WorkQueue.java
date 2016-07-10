package utils;

import java.util.LinkedList;


public class WorkQueue
{
    private final int nThreads;
    private final PoolWorker[] threads;
    private final LinkedList<Runnable> queue;
    public boolean stopped=false;;
    //public ICLSN2 iclsn;

    public WorkQueue(int nThreads)
    {
    	//this.iclsn=iclsn;
        this.nThreads = nThreads;
        queue = new LinkedList<Runnable>();
        threads = new PoolWorker[nThreads];
        //everythingDone=true;
        start();
    }
    
    public void  start(){
    	stopped=false;
    	for (int i=0; i<nThreads; i++) {
            threads[i] = new PoolWorker(i);
            threads[i].start();
        }
    }
    
    

    public void execute(Runnable r) {
        if(stopped){
           start();	
        }
    	synchronized(queue) {
            queue.addLast(r);
            queue.notify();
            //everythingDone=false;
          
        }
    }

    public void stop(){
    	for (int i=0; i<nThreads; i++) {
    		if (threads[i]!=null){
    			threads[i].stop();
    			threads[i].interrupt();
    			threads[i]=null;
    		}
        }
    	stopped=true;
    }
    
    public void finalize(){
    	stop();
    }
    
    private class PoolWorker extends Thread {
    	int id;
    	boolean working;
    	public PoolWorker(int id){
    		this.id=id;
    		working=false;
    	}
        public void run() {
            Runnable r;

            while (true) {
            	working=false;
            	//System.out.println(id+" pret");
                synchronized(queue) {
                    while (queue.isEmpty()) {
                        try
                        {
                        	//System.out.println(id+" attend");
                            queue.wait();
                            working=true;
                        }
                        catch (InterruptedException ignored)
                        {
                        }
                    }

                    r = (Runnable) queue.removeFirst();
                }

                // If we don't catch RuntimeException, 
                // the pool could leak threads
                try {
                	synchronized(r){
                		r.run();
                    
                    	//System.out.println(id+" fini "+((ProbaComputer)r).cascade);
                   
                    	r.notify();
                    }
                    
                }
                catch (RuntimeException e) {
                	System.out.println(e);
                    // You might want to log something here
                }
                
            }
        }
    }
}
