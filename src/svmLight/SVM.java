package svmLight;
import utils.OS_Detector;
import utils.ExecProc;
public class SVM {
  public static long timeout=-1;
	
  
   //params contain all params without prog name and output file
  public static boolean learn(String[] params, String outputFile){
	  String[] all=new String[params.length+2];
	  if (OS_Detector.isWindows()){
		  all[0]="./svm_light_windows/svm_learn";
	  }
	  else{
		  all[0]="./svm_light_linux/svm_learn";
	  }
	  for(int i=0;i<params.length;i++){
		  all[i+1]=params[i];
	  }
	  all[params.length-1]=outputFile;
	  ExecProc exec=new ExecProc(all,timeout);
	  int ret=0;
	  try{
		  ret=exec.executeCommandLine(true,true);
	  }
	  catch(Exception e){
		  System.out.println("Exception :"+e);
		  throw new RuntimeException(e);
	  } 
	  return(true);
  }
  
  // params contain all params without prog name and output file
  public static boolean infer(String[] params, String outputFile){
	  String[] all=new String[params.length+2];
	  if (OS_Detector.isWindows()){
		  all[0]="./svm_light_windows/svm_classify";
	  }
	  else{
		  all[0]="./svm_light_linux/svm_classify";
	  }
	  for(int i=0;i<params.length;i++){
		  all[i+1]=params[i];
	  }
	  all[params.length-1]=outputFile;
	  ExecProc exec=new ExecProc(all,timeout);
	  try{
		  int ret=exec.executeCommandLine(true,true);
	  }
	  catch(Exception e){
		  System.out.println("Exception :"+e);
		  throw new RuntimeException(e);
	  }
	  return(true);
  }
  

  public static void main(String[] args) throws Exception {
	 
  }
}

