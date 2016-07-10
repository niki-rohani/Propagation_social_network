package twitter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;



// Classe pour recuperer des "communaute d'utilisateurs qui on RT ou reply aux memes gars.
public class GetUsersGroupsFromAt {
	
	private int minFollowers ; // Nombre min de follower pour prendre ce gars en centre d'une communaute
	private int nbGroups ; // Nombre de groupe a former (= nombre d'utilisateur initiaux).
	private int maxSize ;
	private String fileprefix ;
	private HashSet<String> toFollow = new HashSet<String>() ;
	private HashMap<String,String> seen = new HashMap<String,String>() ;
	private HashMap<String,Integer> sizes = new HashMap<String, Integer>();
	private int maxTotalUsers;
	private String keywords ;
	
	static private String apiKey = "XHiXT85qzMDPBhZ1Lcpw" ;
	static private String secretKey="5cC66LOsNbPdP8hmFrwsJsZmYsiOChIGxlJuxv9KQqU";
	
	
	public GetUsersGroupsFromAt(int minFollowers, int nbGroups, int maxSize, int maxTotalUsers, String keywords,  String fileprefix) {
		this.minFollowers=minFollowers;
		this.nbGroups=nbGroups;
		this.fileprefix=fileprefix;
		this.maxSize=maxSize;
		this.maxTotalUsers=maxTotalUsers ;
		toFollow = new HashSet<String>();
		seen = new HashMap<String,String>() ;
		this.keywords=keywords ;
	}
	
	
	
	public void lauch() throws FileNotFoundException {
		
		
		OAuthService service = new ServiceBuilder().provider(TwitterApi.class).apiKey(apiKey).apiSecret(secretKey).build();
		Token requestToken = service.getRequestToken() ;
		String authUrl = service.getAuthorizationUrl(requestToken) ;
		System.err.print("URL : "+authUrl+"\nCode de validation : ");
		
		Scanner in = new Scanner(System.in); 
		String id = in.nextLine() ;
		
		Verifier v = new Verifier(id) ;
		Token accessToken = service.getAccessToken(requestToken, v);
		
		OAuthRequest request = new OAuthRequest(Verb.POST, "https://stream.twitter.com/1.1/statuses/sample.json");
		//request.addBodyParameter("track",keywords) ;
		service.signRequest(accessToken, request); // the access token from step 4
		Response response = request.send();
		InputStream inStream = response.getStream() ;
		
		in = new Scanner(inStream) ;
		int nb = 0; 
		String s ;
		while(in.hasNextLine() && this.toFollow.size()<this.nbGroups) {
			s=in.nextLine();
			try {
				//System.out.println(s);
				JSONObject o = (JSONObject) new JSONParser().parse(s) ;
				if(!o.containsKey("user"))
					continue ;
				
				
				JSONObject u = (JSONObject) o.get("user") ;
				//System.out.println(u);
				
				String lang = (String) u.get("lang");
				if(!lang.startsWith("en")) {
					continue ;	
				}
					
				if((Long)(u.get("followers_count"))>=this.minFollowers) {
					toFollow.add((String) u.get("id_str")) ;
					System.out.println("centre : "+(String) u.get("screen_name"));
					sizes.put((String) u.get("id_str"),0) ;
				}
					
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("erf...");
			}
		}
		
		// Arrive ici, on a rempli notre liste de mec "centraux".
		in.close() ;
		
		String userList = "";
		for(String name : toFollow)
			userList=userList+name+",";
		userList.replaceFirst(",$","") ;
		System.out.println(userList);
		
		request = new OAuthRequest(Verb.POST, "https://stream.twitter.com/1.1/statuses/filter.json");
		request.addBodyParameter("follow",userList) ;
		service.signRequest(accessToken, request); // the access token from step 4
		response = request.send();
		inStream = response.getStream() ;
		in = new Scanner(inStream) ;
		while(in.hasNextLine() && seen.size()<maxTotalUsers) {
			s=in.nextLine();
			//System.out.println(s);
			try {
				JSONObject o = (JSONObject) new JSONParser().parse(s) ;
				JSONObject u = (JSONObject) o.get("user") ;
				JSONObject entities = (JSONObject) o.get("entities") ;
				if(!o.containsKey("user"))
					continue ;
				
				String lang = (String) u.get("lang");
				if(!lang.startsWith("en")) {
					continue ;	
				}
				
				String id_str = (String) u.get("id_str") ;
				if(!toFollow.contains(id_str))
					continue ;
				JSONArray mention = (JSONArray) entities.get("user_mentions") ;
				if(mention.size()==0)
					continue ;
				JSONObject user0 = (JSONObject) mention.get(0) ;
				String inReply = (String) user0.get("id_str") ;
				System.out.println("reply : "+inReply);
				if (toFollow.contains(inReply))
					continue ;
				//System.out.println("a");	
				if(sizes.get(id_str)>=maxSize)
					continue ;
				//System.out.println("b");
				if(seen.put(inReply,id_str)==null) {
					System.out.println(inReply+" : "+(String) u.get("screen_name"));
					System.out.println(seen.size());
					int i = sizes.get(id_str) ;
					sizes.put(id_str,i+1) ;
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		PrintStream out = new PrintStream(fileprefix) ;
		out.println("KEYWORDS : "+keywords) ;
		for(String u : seen.keySet())
			out.println(u + "\t"+seen.get(u));
		
		System.out.println("Done !");
		
	}
	
	
	
	
	
	// Permet de reformer une fichier de post en regroupant sous un seul users tout les messages d'un groupe.
	public static void regroup(String streamfile, String usersFile, String outFile) throws IOException, ParseException {
		
		BufferedReader f = new BufferedReader(new FileReader(usersFile)) ;
		f.readLine() ;
		HashMap<String,String> groups = new HashMap<String, String>() ;
		String line ;
		long processed = 0 ;
		long actualN = 0 ;
		while((line=f.readLine()) != null) {
			String[] t = line.split("\t") ;
			groups.put(t[0], t[1]) ;
			
		}
		
		f = new BufferedReader(new FileReader(streamfile)) ;
		PrintStream out = new PrintStream(outFile) ;
		
		while((line=f.readLine()) != null) {
			
			//System.out.println(line);
			JSONObject o = (JSONObject) new JSONParser().parse(line) ;
			JSONObject u = (JSONObject) o.get("user") ;
			if(u==null)
				continue;
			String user = (String) u.get("id_str") ;
			String screenname = (String) u.get("screen_name") ;
			String date = (String) o.get("created_at") ;
			String text = (String) o.get("text") ;
			
			processed++ ;
			if((processed % 1000) == 0)
				System.out.println(processed+" ; "+actualN);
			
			String group = groups.get(user) ;
			if(group == null)
				continue ;
			
			text =text.replaceAll("\\s"," ") ;
			out.println(group+"\t"+screenname+"\t"+date+"\t"+text) ;
			actualN++ ;
			//System.out.println(user+"\t"+date+"\t"+text);
			//byte[] b = text.getBytes() ;
			
			
			
		}
		
		
	}
	
	public static void main(String args[]) {
//		GetUsersGroupsFromAt g = new GetUsersGroupsFromAt(1000, 200, 100, 5000,"championsleague,FMI,EU,Thatcher,blizzard,GoT,batman,bitcoins", "groupsKeys") ;
//		try {
//			g.lauch();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		try {
			GetUsersGroupsFromAt.regroup("/local/bourigaults/twitter/stream2.groupsKeys", "groupsKeys", "/local/bourigaults/twitter/stream2.groupsKeys.regrouped") ;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
