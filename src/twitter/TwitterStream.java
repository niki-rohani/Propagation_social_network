package twitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.Random;
import java.util.Scanner;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;


/*
 * Cette classe permet de lancer un stream "filter" de twitter.
 * 
 */

public class TwitterStream {
	
	/*static private String apiKey = "XHiXT85qzMDPBhZ1Lcpw" ;
	static private String secretKey="5cC66LOsNbPdP8hmFrwsJsZmYsiOChIGxlJuxv9KQqU";*/
	static private String apiKey = "wphVSepcE4qaIVkTT1ousA" ;
	static private String secretKey="Xkva0h8FV5tsqR8SlFuiNj3RYmAFgdHzXjAlnNSI";
	static String UserFile = "/home/bourigaults/twitter/persons442bis" ;
	//static String OutputFile = "/local/bourigault/twitter/stream.out" ;
	

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		
		OAuthService service = new ServiceBuilder().provider(TwitterApi.class).apiKey(apiKey).apiSecret(secretKey).build();
		Token requestToken = service.getRequestToken() ;
		String authUrl = service.getAuthorizationUrl(requestToken) ;
		System.err.print("URL : "+authUrl+"\nCode de validation : ");
		
		Scanner in = new Scanner(System.in); 
		String id = in.nextLine() ;
		
		Verifier v = new Verifier(id) ;
		Token accessToken = service.getAccessToken(requestToken, v);
		
		String userList = makeUserList(args[0]) ;
		
		/**
		 * A commenter / decommenter pour suivre le stream sample OU filter.
		 */
		OAuthRequest request = new OAuthRequest(Verb.POST, "https://stream.twitter.com/1.1/statuses/filter.json");
		//OAuthRequest request = new OAuthRequest(Verb.POST, "https://stream.twitter.com/1.1/statuses/sample.json");
		request.addBodyParameter("follow",userList) ;
		//request.addBodyParameter("track","obama") ;
		service.signRequest(accessToken, request); // the access token from step 4
		int milliseconds = 0 ;
		while(true) {
			Response response = request.send();
			InputStream inStream = response.getStream() ;
			//OutputStream fileStream = new FileOutputStream(OutputFile) ;
			int b ;
			//while((b=inStream.read())!=-1) fileStream.write(b) ;
			try {
				while((b=inStream.read())!=-1) { 
					System.out.write(b) ;
				}
				//fileStream.close() ;
			} catch (SocketException e) {
				inStream.close() ;
				try {
					Thread.sleep(milliseconds) ;
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				milliseconds += 250 ;
			}
		}
		
	}


	private static String makeUserList(String file) {
		
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//PrintStream out = new PrintStream(System.out) ;
		Scanner cin = new Scanner(in) ;
		String l ;
		String re = "" ;
		cin.nextLine(); // skip la premiere ligne au cas ou c'ets des meta infos.
		while(cin.hasNextLine()) {
			l=cin.nextLine() ;
			String[] t = l.split("\t") ;
			re=re+t[0]+"," ;
		}
		re.replaceFirst(",$","");
		return re;
	}

}
