package indexation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import postTagger.TagByPrefix;
import cascades.CascadeFeatureProducer;
import cascades.CascadeFeaturer;
import cascades.CascadesSelector;
import cascades.NbUsers;
import cascades.Step;
import strLinkUsers.PostsInSameCascade;
import cascades.CascadesProducer;
import cascades.CumulativeNbPosts;
import cascades.Chi2Feature;
import cascades.NbPosts;
import cascades.NbSteps;

import com.mongodb.BasicDBObject;

import wordsTreatment.TF_Weighter;
import cascades.CascadesTrainTestSetsBuilder;
public class WholeIndexation {
	
	public static void indexTweet09(){
		String db="tweet09";
		String filename="data/tweet09";
		String postsCol="posts_1";
		BasicRawIndexer indexer=new BasicRawIndexer();
		try{
			String stemsCol=indexer.indexStems(db, filename);
			IDFPruner trans=new IDFPruner(2000,db,stemsCol);
			trans.learn();
			postsCol=indexer.indexData(db, filename, new TF_Weighter(db,stemsCol), trans);
			TagByPrefix strTag=new TagByPrefix("://");
		    postsCol=strTag.tagCollection(db,postsCol);
		   
			PostsSelector postSel=new PostsSelector();
			postsCol=postSel.selectPosts(db, postsCol, 10000);
			CascadesProducer cp=new CascadesProducer();
			String cascadesCol=cp.produceCascades(db,postsCol,new BasicDBObject(),0,-1,5);
			// CascadesSelector eventuellement
			String usersCol=(new PostsInSameCascade(true)).linkUsers(db,cascadesCol);
			CascadesTrainTestSetsBuilder.build(db,cascadesCol,0.5);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void indexUsElections5000(){
		String db="usElections5000_hashtag";
		String filename="data/usElections5000";
		String postsCol; //="posts_1";
		String stepsCol="steps_1";
		int minStep=1;
		TwitterStreamIndexer indexer=new TwitterStreamIndexer();
		try{
			String stemsCol=indexer.indexStems(db, filename);
			IDFPruner trans=new IDFPruner(2000,db,stemsCol);
			trans.learn();
			postsCol=indexer.indexData(db, filename, new TF_Weighter(db,stemsCol), trans);
			TagByPrefix strTag=new TagByPrefix("#");
		    postsCol=strTag.tagCollection(db,postsCol);
		   
			PostsSelector postSel=new PostsSelector();
			postsCol=postSel.selectPosts(db, postsCol, 10000);
			CascadesProducer cp=new CascadesProducer();
			String cascadesCol=cp.produceCascades(db,postsCol,new BasicDBObject(),0,-1,5);
			Step.indexeSteps(db,postsCol);
			ArrayList<CascadeFeatureProducer> featurers=new ArrayList<CascadeFeatureProducer>();
			featurers.add(new NbUsers());
			HashMap<Long,Step> steps=Step.loadSteps(db,"steps_3",minStep);
			long duree=Step.getStepLength(steps);
			System.out.println("Nb Steps = "+steps.size()+" Duree step = "+duree);
			//featurers.add(new NbSteps(steps));
			featurers.add(new CumulativeNbPosts(steps,true));
			featurers.add(new Chi2Feature(steps));
			ArrayList<Double> thresholds=new ArrayList<Double>();
			thresholds.add(5.0);
			//thresholds.add(2.0);
			thresholds.add(1500.0);
			thresholds.add(100.0);
			
			CascadeFeaturer cf=new CascadeFeaturer(featurers,thresholds);
			CascadesSelector cs=new CascadesSelector(cf);
			cascadesCol=cs.selectCascades(db, cascadesCol, stepsCol,1,minStep);
			String usersCol=(new PostsInSameCascade(true)).linkUsers(db,cascadesCol);
			CascadesTrainTestSetsBuilder.build(db,cascadesCol,0.5);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	public static void indexMemetracker(){
		String db="memetracker";
		String filename="data/memetracker";
		String postsCol="posts_1";
		String stepsCol="steps_1";
		long maxInterval=-1; //1000*3600*24*7; 
		String cascadesFile="data/memetracker/clust-qt08080902w3mfq5.txt";
		int minStep=1;
		MemetrackerIndexer indexer=new MemetrackerIndexer();
		try{
			String stemsCol=indexer.indexStems(db, filename);
			//String stemsCol="stems_3";
			
			//String stemsCol2="stems_4";
			IDFPruner trans=new IDFPruner(2000,db,stemsCol);
			trans.learn();
			postsCol=indexer.indexData(db, filename, new TF_Weighter(db,stemsCol), trans);
			
			CascadesProducer cp=new MemetrackerCascadesProducer(cascadesFile);
			//CascadesProducer cp=new CascadesProducer();
			String cascadesCol=cp.produceCascadesDistinctTer(db,postsCol,0,-1,2,100);
			//String cascadesCol; //="cascades_5";
			ArrayList<Double> thresholds=new ArrayList<Double>();
			thresholds.add(10.0);
			ArrayList<CascadeFeatureProducer> featurers=new ArrayList<CascadeFeatureProducer>();
			featurers.add(new NbUsers());
			CascadeFeaturer cf=new CascadeFeaturer(featurers,thresholds);
			CascadesSelector cs=new CascadesSelector(cf);
			cascadesCol=cs.selectCascades(db, cascadesCol, 10,minStep);
			cascadesCol=CascadesTrainTestSetsBuilder.build(db,cascadesCol,0.01,0.01,true);
			String usersCol=(new PostsInSameCascade(true)).linkUsers(db,cascadesCol);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void indexICWSMPruned(){
		String db="icwsmPruned";
		String train_filename="data/icwsm-0/cascades_training.txt";
		String test_filename="data/icwsm-0/cascades_test.txt";
		
		String train_contentCascades_filename="data/icwsm-0/cascades_training_profile.txt.prune=2000";
		String test_contentCascades_filename="data/icwsm-0/cascades_test_profile.txt.prune=2000";
		
		IndexTripletCascades train_indexer=new IndexTripletCascades(train_contentCascades_filename);
		IndexTripletCascades test_indexer=new IndexTripletCascades(test_contentCascades_filename);
		
		
		try{
			
			String trainCol=train_indexer.indexData(db, train_filename, null, null);
			String testCol=test_indexer.indexData(db, test_filename, null, null);
			
			String usersCol=(new PostsInSameCascade(true)).linkUsers(db,trainCol);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void indexDigg(){
		String db="digg";
		String train_filename="data/digg/cascades_training.txt";
		String test_filename="data/digg/cascades_test.txt";
		
		String train_contentCascades_filename="data/digg/cascades_training_profile.txt.prune=2000";
		String test_contentCascades_filename="data/digg/cascades_test_profile.txt.prune=2000";
		
		IndexTripletCascades train_indexer=new IndexTripletCascades(train_contentCascades_filename);
		IndexTripletCascades test_indexer=new IndexTripletCascades(test_contentCascades_filename);
		
		
		try{
			
			String trainCol=train_indexer.indexData(db, train_filename, null, null);
			String testCol=test_indexer.indexData(db, test_filename, null, null);
			
			String usersCol=(new PostsInSameCascade(true)).linkUsers(db,trainCol);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Old fashion to index enron.
	 * More details in EnronAllIndexer.
	 */
	public static void indexEnronAll(){
		String db="enronAll";
		String filename="data/enron/all.txt";
		String postsCol="posts_1";
		long maxInterval=1000*3600*24*7*52; 
		EnronAllIndexer indexer=new EnronAllIndexer();
		try{
			String stemsCol=indexer.indexStems(db, filename);
			IDFPruner trans=new IDFPruner(2000,db,stemsCol);
			trans.learn();
			//String stemsCol="stems_2";
			//NoTransform trans=new NoTransform();
			postsCol=indexer.indexData(db, filename, new TF_Weighter(db,stemsCol), trans);
			
			PostsSelector postSel=new PostsSelector();
			postsCol=postSel.selectPosts(db, postsCol, 10000);
			
			
			CascadesProducer cp=new CascadesProducer();
			String cascadesCol=cp.produceCascades(db,postsCol,new BasicDBObject(),0,-1,2,maxInterval);
			// CascadesSelector eventuellement
			String usersCol=(new PostsInSameCascade(true)).linkUsers(db,cascadesCol);
			CascadesTrainTestSetsBuilder.build(db,cascadesCol,0.5);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void indexEnron(){
		String db="enron_Year";
		String filename="data/enron/enron_mail_20110402/maildir";
		String postsCol="posts_1";
		long maxInterval=1000*3600*24*7*52; 
		EnronIndexer indexer=new EnronIndexer();
		try{
			String stemsCol=indexer.indexStems(db, filename);
			IDFPruner trans=new IDFPruner(2000,db,stemsCol);
			trans.learn();
			//String stemsCol="stems_2";
			//NoTransform trans=new NoTransform();
			postsCol=indexer.indexData(db, filename, new TF_Weighter(db,stemsCol), trans);
			
			PostsSelector postSel=new PostsSelector();
			postsCol=postSel.selectPosts(db, postsCol, 10000);
			
			 
			
			CascadesProducer cp=new CascadesProducer();
			String cascadesCol=cp.produceCascades(db,postsCol,new BasicDBObject(),0,-1,2,maxInterval);
			// CascadesSelector eventuellement
			String usersCol=(new PostsInSameCascade(true)).linkUsers(db,cascadesCol);
			CascadesTrainTestSetsBuilder.build(db,cascadesCol,0.5);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void indexLastfmSongs() {
		String db ="lastfm_songs" ;
		String filename="/local/bourigaults/lastfm/lastfm-dataset-1K/sorted" ;
		String postsCol="posts_1";
		
		LastfmIndexer indexer = new LastfmIndexer(false) ;
		
		try{
			
			String trainCol=indexer.indexData(db, filename, null, null);
			/*User.loadUsersFrom(db, "users_1") ;
			String usersCol=(new PostsInSameCascade(true)).linkUsers(db,"cascades_1");
			for(String  u : User.users.keySet()) {
				User.users.get(u).indexInto(db, "users_1");
			}*/
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void indexLastfmArtist() {
		String db ="lastfm_artists" ;
		String filename="/local/bourigaults/lastfm/lastfm-dataset-1K/sortedArtist" ;
		String postsCol="posts_1";
		
		LastfmIndexer indexer = new LastfmIndexer(true) ;
		
		try{
			
			String trainCol=indexer.indexData(db, filename, null, null);
			/*User.loadUsersFrom(db, "users_1") ;
			String usersCol=(new PostsInSameCascade(true)).linkUsers(db,"cascades_1");
			for(String  u : User.users.keySet()) {
				User.users.get(u).indexInto(db, "users_1");
			}*/
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void indexStackOverflow() {
		String db ="stack" ;
		String filename="/local/bourigaults/stackoverflow/stackexchange-stackoverflow/sorted" ;
		//String postsCol="posts_1";
		
		StackIndexer indexer = new StackIndexer("/local/bourigaults/stackoverflow/stackexchange-stackoverflow/users.top",19) ;
		
		try{
			
			String trainCol=indexer.indexData(db, filename, null, null);
			/*User.loadUsersFrom(db, "users_1") ;
			String usersCol=(new PostsInSameCascade(true)).linkUsers(db,"cascades_1");
			for(String  u : User.users.keySet()) {
				User.users.get(u).indexInto(db, "users_1");
			}*/
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void indexKernel() {
		String db ="kernel" ;
		String filename="/local/bourigaults/kernel/lkml_person-thread/sorted" ;
		
		StackIndexer indexer = new StackIndexer("/local/bourigaults/kernel/lkml_person-thread/users.top",11) ;				
		try{
			
			String trainCol=indexer.indexData(db, filename, null, null);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void indexIrvine() {
		String db ="irvine" ;
		String filename="/local/bourigaults/irvine/opsahl-ucforum/sorted" ;
		
		StackIndexer indexer = new StackIndexer("/local/bourigaults/irvine/opsahl-ucforum/users",1) ;
				
		try{
			
			String trainCol=indexer.indexData(db, filename, null, null);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void indexWeibo() {
		String db = "weibo" ;
		String filename="/local/bourigaults/weibo/week1/week1.csv" ;
		
		try {
			WeiboIndexer indexer = new WeiboIndexer("/local/bourigaults/weibo/week1/users2",100) ;
			indexer.indexData(db, filename, null, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	

	private static void indexMemetrackerNEW() {
	
		String db = "memetrackerNew" ;
		String fileprefix = "/local/bourigaults/meme/quotes_" ;
		String fileUsers = "/local/bourigaults/meme/testclustcount" ;
		String fileEnds[] = new String[]{"2008-08.txt.gz","2008-09.txt.gz","2008-10.txt.gz","2008-11.txt.gz","2008-12.txt.gz","2009-01.txt.gz","2009-02.txt.gz","2009-03.txt.gz","2009-04.txt.gz"} ;
		int nbUsers=500 ;
		String cascadesFile="/local/bourigaults/meme/clust-qt08080902w3mfq5.txt.gz" ;
	
		try {
			MemeNewIndexer indexer = new MemeNewIndexer(fileprefix,fileEnds,fileUsers,nbUsers,cascadesFile) ;
			indexer.indexData(db, "", null, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	
	}
	/*public static void indexFacebook() {
		String db = "facebook" ;
		String filename="/local/bourigaults/facebook/facebook-links.withtime.double.sorted.txt" ;
		
		FacebookIndexer indexer = new FacebookIndexer("/local/bourigaults/facebook/users.top") ;
		
		try{
			
			String trainCol=indexer.indexData(db, filename, null, null);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}*/
	
	
	public static void main(String[] args){
		//indexTweet09();
		//indexUsElections5000();
		//indexEnronAll();
		//indexDigg();
		indexMemetracker();
	}
}
