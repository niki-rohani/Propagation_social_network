package thibault.YahooCollect;

import java.io.FileNotFoundException;

public class CollectEvalYahoo {
	
	public static void main(String[] args) throws FileNotFoundException, Exception{
		
		//PolicyCtxtYahoo p = new PoissonThompsonInd(136,1.0);
		PolicyCtxtYahoo p = new LinUCB(136,1.0,2.0);
		//PolicyCtxtYahoo p = new RandomYahoo(6,1.0);
		CollectContextYahoo c = new CollectContextYahoo(p,136);
		c.run();
		
	}

	

}
