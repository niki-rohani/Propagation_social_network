package thibault.testSt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;





import java.util.List;

import com.twitter.Extractor;

import thibault.dynamicCollect.Arm;
import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterObjectFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.ConfigurationBuilder;


public class CrawlerGeoloc {
	
	public static void main(String[] args) {
		 ConfigurationBuilder cb;
		 String AccessToken;
		 String AccessTokenSecret;
		 String ConsumerKey;
		 String ConsumerSecret;
		
		 OAuthAuthorization auth;
		 TwitterStream twitterStream;
		 StatusListener listener;
		 FilterQuery fq;
		 
			//AccessToken = "1433810725-8IV2kuxd2MGtNfsoIvJ1MEusNG4jW7ZsITiK7CR";
			//AccessTokenSecret = "yyHfXkvCk9CCgU0FqvHBK5OmAvORcqdVoKmRBr4UkDlp6";
			//ConsumerKey = "4EWL0odl9EsbLELDSE6hg";
			//ConsumerSecret = "4EJLnbliUksqy4XJWP7CUlhCkE4ZyDaQoPX6tilWOX8";
			
			AccessToken = "2572896588-ixDDCojFM8FDHLQpRlfhnEO9Di3Yd1eeqqbYJMr";
			AccessTokenSecret = "7HwAva730f6MNrnv1fDJvfWDJwodOSMdbIWr6kKy57NCD";
			ConsumerKey = "vogBpBiNcGre5LNckPiaalyLP";
			ConsumerSecret = "eF4Ts06LgO2abpTHlWPYZJs9eDSLanzEVDTKdUin4FIyCFo3tS";
		 
		 cb = new ConfigurationBuilder();
		 cb.setDebugEnabled(true);
		 cb.setOAuthAccessToken(AccessToken);
		 cb.setOAuthAccessTokenSecret(AccessTokenSecret);
		 cb.setOAuthConsumerKey(ConsumerKey);
		 cb.setOAuthConsumerSecret(ConsumerSecret);
		 cb.setJSONStoreEnabled(true);
		 auth = new OAuthAuthorization(cb.build());
		 twitterStream = new TwitterStreamFactory().getInstance(auth);
		 fq = new FilterQuery();

		 FileWriter fw=null;
			try {
				fw = new FileWriter("savedTweetBis.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
			 final BufferedWriter out = new BufferedWriter(fw);
		
		 listener = new StatusListener(){
			 
				public void onStatus(Status status) {
					String lineToWrite="";
					String text=status.getText();
					text=text.replaceAll("\r", " ");
					text=text.replaceAll("\\r", " ");
					text=text.replaceAll("\\\\r", " ");
					text=text.replaceAll("\n", " ");
					text=text.replaceAll("\\n", " ");
					text=text.replaceAll("\\\\n", " ");
					text=text.replaceAll("\t", " ");
					text=text.replaceAll("\\t", " ");
					text=text.replaceAll("\\\\t", " ");
					text=text.replaceAll("  ", " ");
					text=text.replace("^ ", "");
					boolean isRetweet=false;
					boolean isReply=false;
					if(status.getRetweetedStatus()!=null){
						isRetweet=true;
					}
					if(status.getInReplyToScreenName()!=null){
						isReply=true;
					}
					lineToWrite=isRetweet+"\t"+isReply+"\t"+status.getUser().getScreenName()+"\t"+text+"\t"+status.getCreatedAt();
					System.out.println(lineToWrite);
					try {
						out.write(lineToWrite+"\n");
						out.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
					

				}
				public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
					System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
				}

				public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
					System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
				}

				public void onScrubGeo(long userId, long upToStatusId) {
					System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
				}

				public void onStallWarning(StallWarning warning) {
					System.out.println("Got stall warning:" + warning);
				}

				public void onException(Exception ex) {
					ex.printStackTrace();
				}

			};
			
			
			twitterStream.addListener(listener);
			
			double[][] locations =  new double[][]{{  2.136220,48.707634},
												   {2.202996,48.732521}};
			String[] keyWords = new String[]{"#techday","#cealist"};
			long[] ids=new long[]{154132968l};
			
			
			fq.locations(locations);
			//fq.track(keyWords);
			//fq.follow(ids);
			twitterStream.filter(fq);
			
			
			/*long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime <= 10000){

			}*/
	}

}
