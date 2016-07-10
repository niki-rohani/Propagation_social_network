package core;

public class Environnement {
	private static int verbose=1;
	public static void setVerbose(int v){
		verbose=v;
	}
	public static int getVerbose(){
		return(verbose);
	}
}
