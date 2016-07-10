package thibault.dynamicCollect;



import java.util.ArrayList;
import java.util.HashSet;

import com.mongodb.BasicDBObject;

import core.Post;
import core.User;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.conf.ConfigurationBuilder;


public class LiveStatusStream extends Streamer{

	protected ConfigurationBuilder cb;
	
	protected Object lock = new Object();
	
	protected String AccessToken;
	protected String AccessTokenSecret;
	protected String ConsumerKey;
	protected String ConsumerSecret;
	
	protected OAuthAuthorization auth;
	protected TwitterStream twitterStream;
	protected StatusListener listener;
	protected FilterQuery fq;
	protected long usersToStream[];
	public ArrayList<String> languages;
	public HashSet<String> streamdArms;
	public HashSet<String> newArmsNames;
	public HashSet<String> armNames;
	public HashSet<Post> posts;
	public postTreater postTreater;
	public long T;
	public long startTime;


	public LiveStatusStream (int compte) {
		
		switch (compte)
		{
		  case 0: 
				AccessToken = "2572896588-ixDDCojFM8FDHLQpRlfhnEO9Di3Yd1eeqqbYJMr";
				AccessTokenSecret = "7HwAva730f6MNrnv1fDJvfWDJwodOSMdbIWr6kKy57NCD";
				ConsumerKey = "vogBpBiNcGre5LNckPiaalyLP";
				ConsumerSecret = "eF4Ts06LgO2abpTHlWPYZJs9eDSLanzEVDTKdUin4FIyCFo3tS";
			  break;  
		  case 1: 
				AccessToken = "1433810725-8IV2kuxd2MGtNfsoIvJ1MEusNG4jW7ZsITiK7CR";
				AccessTokenSecret = "yyHfXkvCk9CCgU0FqvHBK5OmAvORcqdVoKmRBr4UkDlp6";
				ConsumerKey = "4EWL0odl9EsbLELDSE6hg";
				ConsumerSecret = "4EJLnbliUksqy4XJWP7CUlhCkE4ZyDaQoPX6tilWOX8";
			  break;
		 default:  
			 	AccessToken = "2572896588-ixDDCojFM8FDHLQpRlfhnEO9Di3Yd1eeqqbYJMr";
				AccessTokenSecret = "7HwAva730f6MNrnv1fDJvfWDJwodOSMdbIWr6kKy57NCD";
				ConsumerKey = "vogBpBiNcGre5LNckPiaalyLP";
				ConsumerSecret = "eF4Ts06LgO2abpTHlWPYZJs9eDSLanzEVDTKdUin4FIyCFo3tS";
		}
		cb = new ConfigurationBuilder();
		cb.setOAuthAccessToken(AccessToken);
		cb.setOAuthAccessTokenSecret(AccessTokenSecret);
		cb.setOAuthConsumerKey(ConsumerKey);
		cb.setOAuthConsumerSecret(ConsumerSecret);
		auth = new OAuthAuthorization(cb.build());
		twitterStream = new TwitterStreamFactory().getInstance(auth);
		fq = new FilterQuery();
		languages = new ArrayList<String>();
		newArmsNames=new HashSet<String>();
		armNames=new HashSet<String>();
		streamdArms=new HashSet<String>();
		posts = new HashSet<Post>();
		postTreater=new postTreater();
		this.setListener();
	}

	public void setListener(){
		listener = new StatusListener(){
			public void onStatus(Status status) {
				
				
				/*if (System.currentTimeMillis() - startTime >= T*1000){
					synchronized (lock) {
			            lock.notify();
			          }
				}*/
				
				//if(languages.contains(status.getLang())){
				User owner=null;
				Post p=null;

					if(status.isRetweet()){
						if(streamdArms.contains(Long.toString(status.getRetweetedStatus().getUser().getId()))){
						//System.out.println("I am Retweeted");
						owner = User.getUser(Long.toString(status.getRetweetedStatus().getUser().getId()));
						p = new Post(status.getText(),owner,status.getCreatedAt().getTime(),null,null);//new BasicDBObject("nbRT", status.getRetweetedStatus().getRetweetCount()).append("idOrigin", status.getRetweetedStatus().getId()));
						posts.add(p);
							
						armNames.add(Long.toString(status.getUser().getId()));
						/*if(armNames.contains(Long.toString(status.getUser().getId()))==false){
							armNames.add(Long.toString(status.getUser().getId()));
							newArmsNames.add(Long.toString(status.getUser().getId()));
							}*/
						}
						
						else{
						//System.out.println("I Retweet");
						owner = User.getUser(Long.toString(status.getUser().getId()));
						p = new Post(status.getText(),owner,status.getCreatedAt().getTime(),null,null);
						posts.add(p);
						
						armNames.add(Long.toString(status.getRetweetedStatus().getUser().getId()));
						/*if(armNames.contains(Long.toString(status.getRetweetedStatus().getUser().getId()))==false){
							armNames.add(Long.toString(status.getRetweetedStatus().getUser().getId()));
							newArmsNames.add(Long.toString(status.getRetweetedStatus().getUser().getId()));
							}*/
						}
						
						/*System.out.println(
								
								"isRetweet: true" + "\t" +"nbRetweetOrigin: " +status.getRetweetedStatus().getRetweetCount()+"\t"+"originalUserName: "+status.getRetweetedStatus().getUser().getScreenName()+"\t"+"originalUserId: "+status.getRetweetedStatus().getUser().getId()+"\n"+
								"UserName: "+status.getUser().getScreenName() + "\t" +"UserId: "+status.getUser().getId() + "\n" +
								//"isRetweeted: "+status.isRetweeted() + "\t" +"nbRetweet: "+status.getRetweetCount()+ "\n" +
								//"isFavorited: "+status.isFavorited() + "\t" + "nbFavorited: "+status.getFavoriteCount() +"\n"+
								"OriginalTweet: "+status.getRetweetedStatus().getText() + "\n"+
								"\n"+"-----------------------------------------------");*/
					}
					
					else if(status.isRetweet()==false && status.getInReplyToScreenName()!=null){
						
						if(streamdArms.contains(Long.toString(status.getInReplyToUserId()))){
						//System.out.println("I am Replied to");
						owner = User.getUser(Long.toString(status.getInReplyToUserId()));
						p = new Post(status.getText(),owner,status.getCreatedAt().getTime(),null,null);
						posts.add(p);
						
						armNames.add(Long.toString(status.getUser().getId()));
						/*if(armNames.contains(Long.toString(status.getUser().getId()))==false){
							armNames.add(Long.toString(status.getUser().getId()));
							newArmsNames.add(Long.toString(status.getUser().getId()));
							}*/
						}
						
						else{
						//System.out.println("I Reply to");
						owner = User.getUser(Long.toString(status.getUser().getId()));
						p = new Post(status.getText(),owner,status.getCreatedAt().getTime(),null,null);
						posts.add(p);
						
						armNames.add(Long.toString(status.getInReplyToUserId()));
						/*if(armNames.contains(Long.toString(status.getInReplyToUserId()))==false){
							armNames.add(Long.toString(status.getInReplyToUserId()));
							newArmsNames.add(Long.toString(status.getInReplyToUserId()));
							}*/
						}
						
						/*System.out.println(
								"isReplyTo: true"+"\t"+"inReplyToName: "+status.getInReplyToScreenName()+"\t"+"inReplyToId: "+status.getInReplyToUserId()+"\n"+
								"UserName: "+status.getUser().getScreenName() + "\t" +"UserId: "+status.getUser().getId() + "\n" +
								//"isRetweeted: "+status.isRetweeted() + "\t" +"nbRetweet: "+status.getRetweetCount()+ "\n" +
								//"isFavorited: "+status.isFavorited() + "\t" + "nbFavorited: "+status.getFavoriteCount() +"\n"+
								"Reply: "+status.getText() +
								"\n"+"-----------------------------------------------");*/
					}
					
					else if(status.isRetweet()==false && status.getInReplyToScreenName()==null){
						//System.out.println("I tweet");
						owner = User.getUser(Long.toString(status.getUser().getId()));
						p = new Post(status.getText(),owner,status.getCreatedAt().getTime(),null,null);
						posts.add(p);
						/*System.out.println(
								"UserName: "+status.getUser().getScreenName() + "\n" +"UserId: "+status.getUser().getId() + "\n" +
								//"isRetweeted: "+status.isRetweeted() + "\t" +"nbRetweet: "+status.getRetweetCount()+ "\n" +
								//"isFavorited: "+status.isFavorited() + "\t" + "nbFavorited: "+status.getFavoriteCount() +"\n"+
								"Tweet: "+status.getText() +
								"\n"+"-----------------------------------------------");*/
					}


				//}
			}
			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
				//System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
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
	}

	public void setFilter( long users[]){
		usersToStream = users;
	}

	public void addLanguage(String lang){
		languages.add(lang);
	}


	public HashSet<Post> streamWhileT(HashSet<String> users, long T){
		this.T=T;
		lock = new Object();
		streamdArms=users;
		newArmsNames= new HashSet<String>();
		posts = new HashSet<Post>();
		long usersId[] = new long[users.size()];
		int i=0;
		for(String userName:users){
			//System.out.println(userName+"\t"+Long.parseLong(userName));
			usersId[i]=Long.parseLong(userName);
			i++;
		}

		this.setFilter(usersId);
		startTime = System.currentTimeMillis();
		fq.follow(usersToStream);
		twitterStream.addListener(listener);
		twitterStream.filter(fq);  
		while (System.currentTimeMillis() - startTime <= T*1000){

		}

	    /*try {
	        synchronized (lock) {
	          lock.wait();
	        }
	      } catch (InterruptedException e) {
	        e.printStackTrace();
	      }*/
		/*twitterStream.clearListeners();
		twitterStream.cleanUp();*/
		twitterStream.shutdown();
		startTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - startTime <= 10000){

		}

		for(Post p :posts){
				postTreater.treatPost(p);
		}

		return posts;
	}




	@Override
	public long getT() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void reinitStreamer() {
		// TODO Auto-generated method stub
		
	}
	
	public HashSet<String> getNewArms(){
		return newArmsNames;
	}
	
	public HashSet<String> getArmNames(){
		return armNames;
	}
	
	public String toString(){
		return "DataBaseStreamerLive";
	}
	

}
