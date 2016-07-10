package thibault.dynamicCollect;

import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;

public class NLP {
	 static StanfordCoreNLP pipeline;

	    public static void init() {
	    	Properties properties = new Properties();
	    	properties.put("annotators", "tokenize, ssplit, parse, sentiment");
//	        pipeline = new StanfordCoreNLP("MyPropFile.properties");
	    	pipeline =  new StanfordCoreNLP(properties);
	    }

	    public static int findSentiment(String tweet) {
	    	 int mainSentiment = 0;
	 	        if (tweet != null && tweet.length() > 0) {
		            int longest = 0;
		            Annotation annotation = pipeline.process(tweet);
		            for (CoreMap sentence : annotation
		                    .get(CoreAnnotations.SentencesAnnotation.class)) {
		                Tree tree = sentence
		                        .get(SentimentCoreAnnotations.AnnotatedTree.class);
		                int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
		                String partText = sentence.toString();
		                if (partText.length() > longest) {
		                    mainSentiment = sentiment;
		                    longest = partText.length();
		                }
		            }
		        }

	        return mainSentiment;
	    }
}
