package twitter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



// Construire une liste en crawlant les followers/follow.
public class FollowListStrat implements UserListStrat {

	/* Fichier "seed
	 * Format du fichier :
	 * SCREEN_NAME1 USER_ID1
	 * SCREEN_NAME2 USER_ID2
	 * ...
	 */
	private String seed ;
	private HashMap<String,Integer> seen ;
	private double pFollows ;
	private double pFollowing;
	private int maxFollow;
	private int maxTotal ;
	
	public FollowListStrat(String seedFile,double d , int maxTotal, int maxFollow) {
		seed = seedFile ;
		seen=new HashMap<String,Integer>() ;
		this.pFollows=d ;
		this.maxTotal=maxTotal ;
		this.maxFollow=maxFollow ;
	}
	
	@Override
	public void createList(String fileName) throws IOException {

		FileInputStream in = new FileInputStream(seed) ;
		PrintStream out = new PrintStream(fileName) ;
		//PrintStream out = new PrintStream(System.out) ;
		Scanner cin = new Scanner(in) ;
		String l ;
		while(cin.hasNextLine()) {
			l=cin.nextLine() ;
			String[] t = l.split(" ") ;
			seen.put(t[0],Integer.parseInt(t[1])) ;
			out.println(t[1]+"\t"+t[0]+"\tSEEDED\t-1\t-") ;
			
			
			Document doc = Jsoup.connect("https://twitter.com/"+t[0]+"/followers").get();
			//System.out.println(doc);
			Elements es = doc.select("div.content-main").get(0).select("div.account") ;
			
			Iterator<Element> iter = es.iterator() ;
			while(iter.hasNext()) {
				Element e = iter.next() ;
				if(seen.put(e.attributes().get("data-screen-name"),Integer.parseInt(e.attributes().get("data-user-id")))==null){
					out.print(e.attributes().get("data-user-id"));
					out.print("\t"+e.attributes().get("data-screen-name"));
					out.println("\tFOLLOWS\t"+t[1]+"\t"+t[0]); 
				}
			}
			
		}
		
		Random r = new Random(); 
		
		while(seen.size()<maxTotal) {
			ArrayList<String> keys = new ArrayList<String>(seen.keySet()) ;
			String name = keys.get(r.nextInt(keys.size())) ;
			int id = seen.get(name) ;
			
			String type = (r.nextFloat()<=pFollows) ? "following" : "followers" ;
			
			Document doc = Jsoup.connect("https://twitter.com/"+name+"/"+type).get();
			//System.out.println(doc);
			Elements es = doc.select("div.content-main").get(0).select("div.account") ;
			Iterator<Element> iter = es.iterator() ;
			int n = 0 ;
			while(iter.hasNext() && n<maxFollow) {
				Element e = iter.next() ;
				if(seen.put(e.attributes().get("data-screen-name"),Integer.parseInt(e.attributes().get("data-user-id")))==null && seen.size()<maxTotal){
					out.print(e.attributes().get("data-user-id"));
					out.print("\t"+e.attributes().get("data-screen-name"));
					out.println("\t"+((type=="followers")?"FOLLOWS":"FOLLOWED-BY")+"\t"+id+"\t"+name); 
					n++ ;
				}
			}
			
		}
		

	}
	
	
	

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		FollowListStrat strat = new FollowListStrat("/home/bourigaults/twitter/seed1",0.5,5000,10) ;
		strat.createList("twitterIdLists/persons.55.5000") ;
	}

}
