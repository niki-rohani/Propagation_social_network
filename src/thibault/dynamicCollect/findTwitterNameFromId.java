package thibault.dynamicCollect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.ConfigurationBuilder;

public class findTwitterNameFromId {
	

	public static void main(String[] args) throws TwitterException, IOException {
		String AccessToken = "2572896588-ixDDCojFM8FDHLQpRlfhnEO9Di3Yd1eeqqbYJMr";
		String AccessTokenSecret = "7HwAva730f6MNrnv1fDJvfWDJwodOSMdbIWr6kKy57NCD";
		String ConsumerKey = "vogBpBiNcGre5LNckPiaalyLP";
		String ConsumerSecret = "eF4Ts06LgO2abpTHlWPYZJs9eDSLanzEVDTKdUin4FIyCFo3tS";
		OAuthAuthorization auth;
		Twitter twitter;
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthAccessToken(AccessToken);
		cb.setOAuthAccessTokenSecret(AccessTokenSecret);
		cb.setOAuthConsumerKey(ConsumerKey);
		cb.setOAuthConsumerSecret(ConsumerSecret);
		auth = new OAuthAuthorization(cb.build());
		twitter = new TwitterFactory().getInstance(auth);
		
		//Va lire les id dans le fichier texte
		HashMap<String,String> Ids = new HashMap<String,String>();
		String fileNameRead = "Ids";
		InputStream ips=null;
		ips = new FileInputStream(fileNameRead);
		InputStreamReader ipsr=new InputStreamReader(ips);
		BufferedReader br=new BufferedReader(ipsr);
		String ligne;
		br.readLine();
		while ((ligne=br.readLine())!=null){
			//String[] str = ligne.split("\t") ;
			String name= twitter.showUser(Long.parseLong(ligne)).getScreenName();
			Ids.put(ligne,name);
			System.out.println(ligne+" "+name);
		}

		
		 String fileNameWrite = "test.txt";
		 FileWriter fw=null;
		 fw = new FileWriter(fileNameWrite);
		 BufferedWriter out = new BufferedWriter(fw);

		 for (String s:Ids.keySet()){
			 out.write(s+"\t"+Ids.get(s)+"\n");
		 }

		out.close();
	
	}

}
