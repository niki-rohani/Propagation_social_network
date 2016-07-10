package twitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class KeywordStreamListStrat implements UserListStrat {

	static private String apiKey = "XHiXT85qzMDPBhZ1Lcpw" ;
	static private String secretKey="5cC66LOsNbPdP8hmFrwsJsZmYsiOChIGxlJuxv9KQqU";
	private int max ;
	private String keys ;
	private HashMap<String, Integer> seen;
	private HashMap<String, Integer> count ;
	private int minposts ;
	
	public KeywordStreamListStrat(String keywords,int maxUser,int minposts) {
		this.max=maxUser ;
		this.keys=keywords ;
		seen=new HashMap<String,Integer>() ;
		count = new HashMap<String, Integer>();
		this.minposts=minposts ;
	}
	
	@Override
	public void createList(String fileName) throws IOException {
		
		PrintStream out = new PrintStream(fileName) ;
		
		OAuthService service = new ServiceBuilder().provider(TwitterApi.class).apiKey(apiKey).apiSecret(secretKey).build();
		Token requestToken = service.getRequestToken() ;
		String authUrl = service.getAuthorizationUrl(requestToken) ;
		System.err.print("URL : "+authUrl+"\nCode de validation : ");
		
		Scanner in = new Scanner(System.in); 
		String id = in.nextLine() ;
		
		Verifier v = new Verifier(id) ;
		Token accessToken = service.getAccessToken(requestToken, v);
		
		OAuthRequest request = new OAuthRequest(Verb.POST, "https://stream.twitter.com/1.1/statuses/filter.json");
		request.addBodyParameter("track",keys) ;
		service.signRequest(accessToken, request); // the access token from step 4
		Response response = request.send();
		InputStream inStream = response.getStream() ;
		
		in = new Scanner(inStream) ;
		int nb = 0; 
		while(in.hasNextLine() && nb < this.max) {
			String s = in.nextLine() ;
			System.out.println(s);
			try {
				JSONObject o = (JSONObject) new JSONParser().parse(s) ;
				JSONObject u = (JSONObject) o.get("user") ;
				if(seen.put((String)u.get("screen_name"), Integer.parseInt((String)u.get("id_str")))==null) {
					count.put((String)u.get("screen_name"), 1) ;
					if(minposts==1) {
						out.println(u.get("id_str")+"\t"+u.get("screen_name"));
						nb++;
					}
				} else {
					int nbseen = count.get((String)u.get("screen_name")); 
					count.put((String)u.get("screen_name"), nbseen+1) ;
					if(nbseen+1==minposts){
						out.println(u.get("id_str")+"\t"+u.get("screen_name"));
						nb++;
					}
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}

	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		KeywordStreamListStrat k = new KeywordStreamListStrat(args[0],5000,Integer.parseInt(args[1])) ;
		k.createList(args[2]) ;
		//TODO lance ca !
	}

}
