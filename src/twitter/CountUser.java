package twitter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/*
 * C'est juste pour contern le nombre d'utilisateur qu'on a choppe dans un stream.
 */
public class CountUser {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		
		FileInputStream in = new FileInputStream("/home/bourigaults/workspace/Propagation2/twitterIdLists/persons.usa2012.5000") ;
		PrintStream out = new PrintStream("/local/bourigaults/twitter/streamTestUSA_2.filtered5000") ;
		Scanner cin = new Scanner(in) ;
		String l ;
		String name ;
		
		HashSet<String> users = new HashSet<String>() ;
		
		while(cin.hasNextLine()) {
			l=cin.nextLine() ;
			String[] t=l.split("\t") ;
			users.add(t[1]) ;
		}
		
		
		in = new FileInputStream("/local/bourigaults/twitter/streamTestUSA_2") ;
		cin = new Scanner(in) ;
		
		while(cin.hasNextLine()) {
			l=cin.nextLine() ;
			try {
				JSONObject o = (JSONObject) new JSONParser().parse(l) ;
				JSONObject u = (JSONObject) o.get("user") ;
				//System.out.println((String)u.get("screen_name"));
				name = (String)u.get("screen_name") ;
				if(users.contains(name))
					out.println(l);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}

	}

}
