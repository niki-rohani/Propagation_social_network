package topicModels;

import java.util.HashMap;
import java.util.HashSet;
import core.Text;
import core.Model;

public abstract class TopicIdentificator {
	public abstract HashMap<Text,HashMap<Integer,Double>> inferTopics(HashSet<Text> texts);
}
