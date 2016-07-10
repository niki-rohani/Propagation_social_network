package postTagger;

import java.util.HashSet;

import actionsBD.MongoDB;
import core.Post;
import core.Text;

import topicModels.TopicIdentificator;
import java.util.HashMap;
import java.util.ArrayList;

import com.mongodb.DBCollection;

import similarities.Cosine;
import topicModels.ClusterModel;
import topicModels.ClusterAssignment;

public class TagByTopic extends PostTagger {

	private TopicIdentificator strTopic;
	
	public TagByTopic(TopicIdentificator strTopic){
		this.strTopic=strTopic;
	}
	
	
	@Override
	public void tagPosts(HashSet<Post> posts) {
		HashMap<Text,HashMap<Integer,Double>> itopics=strTopic.inferTopics(new HashSet<Text>(posts));
		for(Text t:itopics.keySet()){
			Post p=(Post)t;
			System.out.println(p);
			HashMap<Integer,Double> topics=itopics.get(t);
			for(Integer i:topics.keySet()){
				double val=topics.get(i);
				if (val>0.0){
					System.out.println("T"+i+"="+val);
					p.addTag("T"+i,val);
				}
			}
		}
	}
	
	public String toString(){
		return("TagByTopic selon "+strTopic);
	}
	
	/*@Override
	public void linkPosts(HashSet<Post> posts) {
		//HashMap<Integer,HashMap<Post,Double>> topics=new HashMap<Integer,HashMap<Post,Double>>();
		HashMap<Text,HashMap<Integer,Double>> itopics=strTopic.inferTopics(new HashSet<Text>(posts));
		Cosine cos=new Cosine();
		ArrayList<Text> texts=new ArrayList<Text>(itopics.keySet());
		for(int i=0;i<texts.size()-1;i++){
			Text t1=texts.get(i);
			Post p1=(Post)t1;
			HashMap<Integer,Double> h1=itopics.get(t1);
			boolean allSims=false;
			if (h1.size()>1){
				allSims=true;
			}
			for(int j=1;j<texts.size();j++){
				Text t2=texts.get(j);
				Post p2=(Post)t2;
				HashMap<Integer,Double> h2=itopics.get(t2);
				// On calcule la concordance de topics par un cosine
				double sim=cos.computeSim(h1, h2);
				if (sim>0.0){
					try{
						
						String type=allSims?"sims":"T"+h2.keySet().iterator().next();
						
						p2.addReference(type,p1,sim);
					}
					catch(Exception e){
						System.out.println("Exception impossible : "+e);
						throw new RuntimeException(e);
					}
				}
			}
		}
	}*/
	
	public static void main(String[] args){
		try{
			//ClusterAssignment cl=new ClusterAssignment("ClusterModels/posts1KMeans_ratio=0.001_k=100.txt",new Cosine());
			ClusterAssignment cl=new ClusterAssignment(args[0],new Cosine());
			
			TagByTopic strTag=new TagByTopic(cl);	
			strTag.tagCollection(args[1],args[2]);
			//strTag.tagCollection("trec11","posts1_TFIDF_Trec11", "posts3_posts1KMeans_ratio0o001_k100");
		}
		catch(Exception e){
			System.out.println(e);
		}
		//DBCollection col=MongoDB.mongoDB.getCollection("posts3_posts1KMeans_ratio=0001_k=100");
		//col.drop();
	}

}
