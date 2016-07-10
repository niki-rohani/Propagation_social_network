package cascades;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashMap;

import actionsBD.MongoDB;
import java.util.List;
import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import core.Post;
import core.User;
import java.util.HashSet;
public class CascadesProducer {
	public Cascade produceCascade(String tag,String postsDB,String postsCollection){
		return(produceCascade(tag,postsDB,postsCollection,new BasicDBObject()));
	}
	public Cascade produceCascade(String tag,String db, String postsCollection,BasicDBObject query){
		System.out.println("Produce Cascade for Tag : "+tag);
		String squery="";
		if (query.keySet().size()>0){
			squery="_"+query.toString();
		}
		Cascade ret=new Cascade(tag+squery,db,postsCollection);
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(db,postsCollection);
		DBCursor cursor=null;
		//System.out.println("Query "+query);
		int nb=0;
		query.put("tags.tag", tag);
		System.out.println("Query "+query);
		try {
			cursor= col.find(query).addOption(Bytes.QUERYOPTION_NOTIMEOUT);
			
			while(cursor.hasNext()) {
				DBObject res=cursor.next();
				String nown=res.get("owner").toString();
				User owner=User.getUser(nown);
				int id=Integer.parseInt(res.get("id").toString());
				Post p=Post.getPost(id);
				if(p==null){
					p=new Post(id,owner,Long.valueOf(res.get("timestamp").toString()));
				}
				ret.addPost(p);
				nb++;
				if ((nb%10000)==0){
					System.gc();
				}
			}
		} finally {
			query.removeField("tags.tag");
			if (cursor!=null){
				cursor.close();
				cursor=null;
				System.gc();
			}
		}
		//System.out.println("Query "+query);
		System.out.println(ret);
		return(ret);
	}
	
	public ArrayList<Cascade> produceCascades(List<String> tags,String db, String postsCollection,BasicDBObject query){
		ArrayList<Cascade> cascades=new ArrayList<Cascade>();
		for(String tag:tags){
			//System.out.println("Tag : "+tag);
			Cascade c=produceCascade(db,tag,postsCollection,query);
			if(c!=null){
				cascades.add(c);
			}
		}
		return(cascades);
	}
	/*public ArrayList<Cascade> produceCascades(String inputDB,String inputCol){
		return(produceCascades(inputDB,inputCol,"",""));
	}*/
	/*public ArrayList<Cascade> produceCascades(String inputDB,String inputCol,String outputCol){
		return(produceCascades(inputDB,inputCol,0,-1,inputDB,outputCol));
	}*/
	public String produceCascades(String inputDB,String inputCol){
		//System.out.println("here");
		return(produceCascades(inputDB,inputCol,new BasicDBObject(),0,-1,1));
	}
	/*public ArrayList<Cascade> produceCascades(String inputDB,String inputCol,int debut, int nb,String outputDB,String outputCol){
		return(produceCascades(inputDB,inputCol,new BasicDBObject(),debut,nb,outputDB,outputCol));
	}*/
	
	/*private List<String> getDistinctTags(DBCollection col){
		 try{
			 List<String> ret=(List<String>)col.distinct("tags.tag");
			 return ret;
		 }
		 catch(Exception e){
			 System.out.println("\nDistinct too big, trying something else...");
			 HashSet<String> tags=new HashSet<String>();
			 DBCursor cursor = col.find(new BasicDBObject());
			 try {
			    //HashMap<Integer,ArrayList<DBObject>> relations=new HashMap<Integer,ArrayList<DBObject>>();
				while(cursor.hasNext()) {
			 		DBObject res=cursor.next();
			 		ArrayList<DBObject> ltags=(ArrayList<DBObject>)res.get("tags");
					//HashMap<Integer,Double> tags=new HashMap<Integer,Double>();
					for(DBObject obj:ltags){
						String tag=obj.get("tag").toString(); //keySet().iterator().next();
						//double val=Double.valueOf(obj.get("val").toString());
						//p.addTag(tag, val);
						tags.add(tag);
					}
				}
			  }
			  finally {
					cursor.close();
			  }
			  return new ArrayList<String>(tags);
		 }
	}*/
	
	public String produceCascades(String inputDB,String inputCol,BasicDBObject query,int debut, int nb,int nbMinUsers){
		return(produceCascades(inputDB,inputCol,query,debut,nb,nbMinUsers,-1));
	}
	
		
	public String produceCascades(String inputDB,String inputCol,BasicDBObject query,int debut, int nb, int nbMinUsers, long maxInterval){
		System.out.println("Produce cascades from "+inputDB+":"+inputCol+" query="+query.toString()+" debut="+debut+" nb="+nb+ "nbMinUsers="+nbMinUsers+", maxInterval = "+maxInterval);
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(inputDB,inputCol);	
		DBCursor cursor = col.find(query);
		HashMap<String,TreeMap<Long,HashSet<Integer>>> tags=new HashMap<String,TreeMap<Long,HashSet<Integer>>>();
		HashMap<Integer,String> posts=new HashMap<Integer,String>();
		try {
		    //HashMap<Integer,ArrayList<DBObject>> relations=new HashMap<Integer,ArrayList<DBObject>>();
			int n=0;
			int nbTraites=0;
			while(cursor.hasNext()) {
			  Post.reinitPosts();
			  User.reinitUsers();
			  if (n>=debut){
		 		DBObject res=cursor.next();
		 		Post p=Post.getPostFrom(res,true);
		 		int id=p.getID();
		 		String owner=p.getOwner().getName();
		 		Long timestamp=p.getTimeStamp();
		 		HashMap<String,String> ptags=p.getTags();
		 		for(String t:ptags.keySet()){
					
		 			TreeMap<Long,HashSet<Integer>> us=tags.get(t);
					if(us==null)
					{
						us=new TreeMap<Long,HashSet<Integer>>();
						tags.put(t,us);
					}
					HashSet<Integer> pos=us.get(timestamp);
					if(pos==null){
						pos=new HashSet<Integer>();
						us.put(timestamp, pos);
					}
					pos.add(id);
					posts.put(id,owner);
					
		 			
				}
		 		nbTraites++;
		 		if(nbTraites%1000==0){
		 			System.out.println("nb traites = "+nbTraites);
		 		}
			  }
			  n++;
			  if((nb>0) && (nbTraites>nb)){
				  break;
			  }
			}
		  }
		  finally {
				cursor.close();
		  }
		  String outputCol=MongoDB.mongoDB.createCollection(inputDB,"cascades","cascades from "+inputDB+":"+inputCol+" query="+query.toString()+" debut="+debut+" nb="+nb+ " nbMinUsers="+nbMinUsers+" maxInterval="+maxInterval);
		  System.out.println("nb tags = "+tags.keySet().size());
		  int cas=0;
		  for(String tag:tags.keySet()){
			  Post.reinitPosts();
			  User.reinitUsers();
			  /*System.out.println(tag+" : "+tags.get(tag));
			  System.out.println(tag+" : "+posts.get(tag));
			  */
			  /*boolean ok=false;
			  if(posts.get(tag).size()>1){
				  System.out.println(tag+" : "+tags.get(tag));
				  System.out.println(tag+" : "+posts.get(tag));
				  ok=true;  
			  }*/
			  
			  TreeMap<Long,HashSet<Integer>> times=tags.get(tag);
			  long lastT=-1;
			  ArrayList<HashMap<String,Integer>> cascs=new ArrayList<HashMap<String,Integer>>();
			  int nbC=-1;
			  for(Long t:times.keySet()){
				  HashMap<String,Integer> cur;
				  if((lastT<0) || ((maxInterval>0) && ((t-lastT)>maxInterval))){
					  cur=new HashMap<String,Integer>();
					  cascs.add(cur);
					  nbC++;
				  }
				  cur=cascs.get(nbC);
				  
				  
				  HashSet<Integer> pp=times.get(t);
				  for(Integer id:pp){
					  String owner=posts.get(id);
					  if(!cur.containsKey(owner)){
						  cur.put(owner, id);
					  }
					  
				  }
				  lastT=t;
			  }
			  int i=1;
			  for(HashMap<String,Integer> c:cascs){
				  HashSet<Post> pos=new HashSet<Post>();
				  if(c.size()>nbMinUsers){
					  for(String user:c.keySet()){
						  int v=c.get(user);
						  Post p=new Post(v,""+v, User.getUser(user), 0, null);
						  pos.add(p);
					  }
					  Cascade ca=new Cascade(tag+"_"+i,inputDB,inputCol,pos);
					  System.out.println("index cascades "+tag+"_"+i+" size = "+pos.size());
					  
					  ca.indexInto(inputDB, outputCol);
					  cas++;
					  //System.out.println(tag+" : "+tp);
					  //System.out.println(tag+" => "+pos.size()+" posts");
					  if(cas%100==0){
						  System.out.println(cas+" cascades inserees");
					  }
					  i++;
				  }
				  
			  }
			  
			  
		  }
		  DBCollection outcol=MongoDB.mongoDB.getCollectionFromDB(inputDB,outputCol);
		  outcol.ensureIndex(new BasicDBObject("id", 1));
		  return outputCol;
	}
	
	public String produceCascadesDistinct(String inputDB,String inputCol,int debut, int nb, int nbMinUsers, long maxInterval){
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(inputDB,inputCol);	
		System.out.print("Distinct tags...");
		List<String> tags=null;
		try{
			tags=(List<String>)col.distinct("tags.tag");;
		}
		 catch(Exception e){
			 System.out.println("\nDistinct too big, trying something else...");
			 return produceCascadesDistinctBis(inputDB,inputCol,debut, nb, nbMinUsers, maxInterval);
			 
		 }
		System.out.println("ok");
		
		//ArrayList<Cascade> cascades=null;
		//if (outputCol.length()==0){cascades=new ArrayList<Cascade>();}
		String outputCol=MongoDB.mongoDB.createCollection(inputDB,"cascades","cascades of tags from "+inputDB+":"+inputCol+" debut="+debut+" nb="+nb+ " nbMinUsers="+nbMinUsers+", maxInterval = "+maxInterval);
		int n=0;
		int nc=0;
		for(String tag:tags){
			Post.reinitPosts();
			User.reinitUsers();
			if (n>=debut){
				//System.out.println("Tag : "+tag);
				Cascade c=produceCascade(tag,inputDB,inputCol,new BasicDBObject());
				String cname=c.name;
				if(c!=null){
					HashSet<Post> posts=c.getPosts();
					TreeMap<Long,HashSet<Post>> tp=new TreeMap<Long,HashSet<Post>>();
					for(Post p:posts){
						long t=p.getTimeStamp();
						HashSet<Post> h=tp.get(t);
						if(h==null){
							h=new HashSet<Post>();
							tp.put(t, h);
						}
						h.add(p);
					}
					long oldt=-1;
					HashMap<Integer,HashSet<Post>> cascs=new HashMap<Integer,HashSet<Post>>();
					HashSet<Post> current=null;
					int ncc=0;
					for(Long t:tp.keySet()){
						if((oldt==-1) || ((maxInterval>0) && (t-oldt>maxInterval))){
							current=new HashSet<Post>();
							ncc++;
							cascs.put(ncc, current);
						}
						current.addAll(tp.get(t));
						oldt=t;
					}
					
					for(Integer numc:cascs.keySet()){
						HashSet<Post> cc=cascs.get(numc);
						// Un seul post par user
						HashMap<Integer,Post> uposts=new HashMap<Integer,Post>();
						
						for(Post p:cc){
							int us=p.getOwner().getID();
							long t=p.getTimeStamp();
							if ((!uposts.containsKey(us)) || (uposts.get(us).getTimeStamp()>t)){
								uposts.put(us, p);
							}
						}
						if(uposts.size()>nbMinUsers){
							c=new Cascade(cname+"_"+numc,inputDB,inputCol);
							c.setPosts(new HashSet<Post>(uposts.values()));
							//if (outputCol.length()>0){
							c.indexInto(inputDB, outputCol);
							//}
						}
					}
					
					//else{
					//	cascades.add(c);
					//}
				}
				nc++;
				if ((nb>0) && (nc>=nb)){
					break;
				}
			}
			n++;
		}
		DBCollection outcol=MongoDB.mongoDB.getCollectionFromDB(inputDB,outputCol);
		outcol.ensureIndex(new BasicDBObject("id", 1));
		//return(cascades);
		return outputCol;
	}
	
	
	public String produceCascadesDistinctBis(String inputDB,String inputCol,int debut, int nb, int nbMinUsers, long maxInterval){
		System.out.print("DistinctBis tags...");
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(inputDB,inputCol);
		DBCursor cursor=null;
		DBCursor cursor2=null;
		
		//System.out.println("Query "+query);
		BasicDBObject query=new BasicDBObject();
		HashSet<String> tags=new HashSet<String>();
		//System.out.println("Query "+query);
		String outputCol=MongoDB.mongoDB.createCollection(inputDB,"cascades","cascades of tags from "+inputDB+":"+inputCol+" debut="+debut+" nb="+nb+ " nbMinUsers="+nbMinUsers+", maxInterval = "+maxInterval);
		int n=0;
		int nc=0;
		Cascade c=null;
		try {
			cursor= col.find(query).addOption(Bytes.QUERYOPTION_NOTIMEOUT);
			int nbposts=0;
			while(cursor.hasNext()) {
				nbposts++;
				DBObject res=cursor.next();
				Post post=Post.getPostFrom(res,true);
				
				System.out.println(nbposts+" posts traites");
				HashMap<String,String> tt=post.getTags();
				for(String tag:tt.keySet()){
					if(tags.contains(tag)){
						System.out.println("Tag "+tag+" deja traite");
						continue;
					}
					tags.add(tag);
					//if(!tags.contains(tag)){
						Post.reinitPosts();
						User.reinitUsers();
						if (n>=debut){
							//System.out.println("Tag : "+tag);
							//Cascade c=produceCascade(tag,inputDB,inputCol,query);
							HashSet<Post> posts=new HashSet<Post>();
							query.put("tags.tag", tag);
							System.out.println("Query "+query);
							try {
								cursor2= col.find(query); //.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
								
								while(cursor2.hasNext()) {
									DBObject res2=cursor2.next();
									String nown=res2.get("owner").toString();
									User owner=User.getUser(nown);
									int id=Integer.parseInt(res2.get("id").toString());
									Post p=Post.getPost(id);
									if(p==null){
										p=new Post(id,owner,Long.valueOf(res2.get("timestamp").toString()));
									}
									posts.add(p);
								}
							} finally {
								query.removeField("tags.tag");
								if (cursor2!=null){
									cursor2.close();
									cursor2=null;
									
								}
							}
							query.removeField("tags.tag");
							String cname=tag;
							if(posts.size()>=nbMinUsers){
								HashMap<Integer,HashSet<Post>> cascs=new HashMap<Integer,HashSet<Post>>();
								
								if(maxInterval>0){
									TreeMap<Long,HashSet<Post>> tp=new TreeMap<Long,HashSet<Post>>();
									for(Post p:posts){
										long t=p.getTimeStamp();
										HashSet<Post> h=tp.get(t);
										if(h==null){
											h=new HashSet<Post>();
											tp.put(t, h);
										}
										h.add(p);
									}
								
									long oldt=-1;
									HashSet<Post> current=null;
									int ncc=0;
									for(Long t:tp.keySet()){
										if((oldt==-1) || ((maxInterval>0) && ((t-oldt)>maxInterval))){
											current=new HashSet<Post>();
											ncc++;
											cascs.put(ncc, current);
										}
										current.addAll(tp.get(t));
										oldt=t;
									}
								}
								else{
									cascs.put(1, posts);
								}
								System.out.println(cascs.size()+" cascades");
								for(Integer numc:cascs.keySet()){
									HashSet<Post> cc=cascs.get(numc);
									System.out.println(cc.size());
									
									// Un seul post par user
									HashMap<Integer,Post> uposts=new HashMap<Integer,Post>();
									
									for(Post p:cc){
										int us=p.getOwner().getID();
										long t=p.getTimeStamp();
										if ((!uposts.containsKey(us)) || (uposts.get(us).getTimeStamp()>t)){
											uposts.put(us, p);
										}
									}
									if(uposts.size()>=nbMinUsers){
										c=new Cascade(cname+"_"+numc,inputDB,inputCol);
										c.setPosts(new HashSet<Post>(uposts.values()));
										//if (outputCol.length()>0){
										c.indexInto(inputDB, outputCol);
										//}
									}
									else{System.out.println(cname+"_"+numc+" trop petite : "+uposts.size()+" users");}
								}
								
								//else{
								//	cascades.add(c);
								//}
							}
							nc++;
							if ((nb>0) && (nc>=nb)){
								break;
							}
						}
						n++;
						
					
				}
				nb++;
				if ((nb%10000)==0){
					System.gc();
				}
			}
		} finally {
			if (cursor!=null){
				cursor.close();
				cursor=null;
				System.gc();
			}
		}
		
		
		DBCollection outcol=MongoDB.mongoDB.getCollectionFromDB(inputDB,outputCol);
		outcol.ensureIndex(new BasicDBObject("id", 1));
		//return(cascades);
		return outputCol;
	}
	
	public String produceCascadesDistinctTer(String inputDB,String inputCol,int debut, int nb, int nbMinUsers, int nbMaxUsers){
		System.out.print("DistinctTer tags...");
		DBCollection col=MongoDB.mongoDB.getCollectionFromDB(inputDB,inputCol);
		DBCursor cursor=null;
		DBCursor cursor2=null;
		
		//System.out.println("Query "+query);
		BasicDBObject query=new BasicDBObject();
		HashSet<String> tags=new HashSet<String>();
		//System.out.println("Query "+query);
		String outputCol=MongoDB.mongoDB.createCollection(inputDB,"cascades","cascades of tags from "+inputDB+":"+inputCol+" debut="+debut+" nb="+nb+ " nbMinUsers="+nbMinUsers);
		int n=0;
		int nc=0;
		Cascade c=null;
		HashMap<String,Long> users=null;
		HashMap<String,Integer> pos=null;
		try {
			cursor= col.find(query).addOption(Bytes.QUERYOPTION_NOTIMEOUT);
			int nbposts=0;
			while(cursor.hasNext()) {
				nbposts++;
				DBObject res=cursor.next();
				Post post=Post.getPostFrom(res,true);
				
				System.out.println(nbposts+" posts traites");
				HashMap<String,String> tt=post.getTags();
				for(String tag:tt.keySet()){
					if(tags.contains(tag)){
						System.out.println("Tag "+tag+" deja traite");
						continue;
					}
					tags.add(tag);
					//if(!tags.contains(tag)){
						Post.reinitPosts();
						User.reinitUsers();
						if (n>=debut){
							//System.out.println("Tag : "+tag);
							//Cascade c=produceCascade(tag,inputDB,inputCol,query);
							HashSet<Post> posts=new HashSet<Post>();
							query.put("tags.tag", tag);
							System.out.println("Query "+query);
							boolean ok=true;
							try {
								cursor2= col.find(query); //.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
								System.out.println("query ok");
								users=new HashMap<String,Long>();
								pos=new HashMap<String,Integer>();
								
								while(cursor2.hasNext()) {
									DBObject res2=cursor2.next();
									Long time=Long.valueOf(res2.get("timestamp").toString());
									String nown=res2.get("owner").toString();
									int id=Integer.parseInt(res2.get("id").toString());
									Long oldt=users.get(nown);
									if((oldt==null) || (oldt>time)){
										users.put(nown,time);
										pos.put(nown,id);
										
									}
									if(users.size()>nbMaxUsers){
										System.out.println(tag+" trop grande : "+users.size());
										ok=false;
										break;
									}
								}
								
							}
							catch(Exception e){
								System.out.println(e);
								continue;
							} finally {
								query.removeField("tags.tag");
								if (cursor2!=null){
									cursor2.close();
									cursor2=null;
									
								}
							}
							if(!ok){
								continue;
							}
							if(users.size()<nbMinUsers){
								System.out.println(tag+" trop petite : "+users.size()+" users");
								continue;
							
							}
							else{
								System.out.println(users.size()+" users");
							}
							for(String user:users.keySet()){
								posts.add(new Post(pos.get(user),null,users.get(user)));
							}
							c=new Cascade(tag,inputDB,inputCol);
							c.setPosts(posts);
							c.indexInto(inputDB, outputCol);
							
							nc++;
							if ((nb>0) && (nc>=nb)){
								break;
							}
						}
						n++;
						
					
				}
				nb++;
				if ((nb%10000)==0){
					System.gc();
				}
			}
		} finally {
			if (cursor!=null){
				cursor.close();
				cursor=null;
				System.gc();
			}
		}
		
		
		DBCollection outcol=MongoDB.mongoDB.getCollectionFromDB(inputDB,outputCol);
		outcol.ensureIndex(new BasicDBObject("id", 1));
		//return(cascades);
		return outputCol;
	}
	
	/*
	 * Produit une cascade pour le test a partir d'une hashmap User->timestamp.s
	 */
	public Cascade fromSimpleHashMap(HashMap<User,Long> c, int id) {
	
		Cascade cascade = new Cascade(id,"rien","rien","rien");
		
		for(User k : c.keySet()) {
			Post p = new Post(null, k, c.get(k), null) ;
			cascade.addPost(p) ;
		}
		
		return cascade ;
		
		
	} 
	
	public static void main(String[] args){
		CascadesProducer cp=new CascadesProducer();
		if(args.length<2){
			System.out.println("Usage : CascadesProducer <db> <posts_collection>");
		}
		cp.produceCascades(args[0],args[1],new BasicDBObject(),0,-1,-1);
		/*DBCollection col=MongoDB.mongoDB.getCollectionFromDB(args[0],args[1]);	
		System.out.print("Distinct tags...");
		List<String> tags=cp.getDistinctTags(col);
		System.out.println("ok\n nb tags= "+tags.size());*/
		/*int nb=0;
		DBCollection col=MongoDB.mongoDB.getCollection("posts2_posts1HashTag");
		DBCursor cursor=null;
		try {
			cursor= col.find().addOption(Bytes.QUERYOPTION_NOTIMEOUT);
			while(cursor.hasNext()) {
				DBObject res=cursor.next();
				//System.out.println(res);
				nb++;
				System.out.println(nb);
				if ((nb%10000000)==0){
					System.gc();
				}
			}
		} finally {
			if (cursor!=null){
				cursor.close();
			}
		}*/
	}
	
}
