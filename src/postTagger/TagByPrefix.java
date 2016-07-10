package postTagger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import actionsBD.MongoDB;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;

public class TagByPrefix extends PostTagger {

	private String prefix="#";
	
	public TagByPrefix(String prefix){
		this.prefix=prefix;
	}
	
	@Override
	public void tagPosts(HashSet<Post> posts) {
		int nbp=posts.size();
		int nb=0;
		for(Post p:posts){
			String texte=p.getTexte();
			Pattern pat = Pattern.compile("("+prefix+"[\\w.-~:=?+&!$,/]+)");
			Matcher m = pat.matcher(texte);
            while(m.find()){
            	//String[] li=texte.split(" ");
            	//HashSet<String> hashtags=new HashSet<String>();
            	//for(String tag:li){
				//if (tag.startsWith(this.prefixHash)){
            	String tag = m.group(1).toLowerCase();
            	p.addTag(tag);
				//}
			}
		}

	}
	
	
	
	
	/*@Override
	public void linkPosts(HashSet<Post> posts) {
		HashMap<String,HashSet<Post>> hashtagsInPost=new HashMap<String,HashSet<Post>>();
		int nbp=posts.size();
		int nb=0;
		for(Post p:posts){
			String texte=p.getTexte();
			String[] li=texte.split(" ");
			//HashSet<String> hashtags=new HashSet<String>();
			for(String tag:li){
				if (tag.startsWith(this.prefixHash)){
					HashSet<Post> tp;
					if (hashtagsInPost.containsKey(tag)){
						tp=hashtagsInPost.get(tag);
					}
					else{
						tp=new HashSet<Post>();
						hashtagsInPost.put(tag, tp);
					}
					for(Post pr:tp){
						try{
							p.addReference(tag,pr,1.0);
						}
						catch(Exception e){
							System.out.println("Cas impossible : "+e);
						}
					}
					tp.add(p);
				}
			}
			nb++;
			if ((nb%100)==0){
				System.out.println(nb+"/"+nbp+" posts traites");
			}
			
		}

	}*/
	public String toString(){
		return("TagByPrefix_"+prefix);
	}
	public static void main(String[] args){
		TagByPrefix strTag=new TagByPrefix("://");
		strTag.tagCollection(args[0],args[1]);
		/*DBCollection outcol=MongoDB.mongoDB.getCollectionFromDB(args[0],args[1]);
		outcol.ensureIndex(new BasicDBObject("tags.tag", 1));
		outcol.ensureIndex(new BasicDBObject("id", 1));*/
	}

}
