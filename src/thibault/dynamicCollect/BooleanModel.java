package thibault.dynamicCollect;

import java.util.HashSet;

import core.Post;

public class BooleanModel{
	Op op;
	BooleanModel(Op op){
		this.op=op;
	}
	boolean eval(Post p){
		return op.eval(p);
	}
	public String toString(){
		return op.toString();
	}
}

abstract class Op{
	abstract boolean eval(Post p);
}
class Word extends Op{
	Integer word;
	Word(Integer word){
		this.word=word;
	}
	boolean eval(Post p){
		Double d=p.getWeights().get(word);
		if(d!=null){
			if(d>0.0){
				return true;
			}
		}
		return false;
	}
	public String toString(){
		return ""+word;
	}
}
class And extends Op{
	HashSet<Op> ops;
	And(HashSet<Op> ops){
		this.ops=ops;
	}
	boolean eval(Post p){
		for(Op op:ops){
			if(!op.eval(p)){
				return false;
			}
		}
		return true;
	}
	public String toString(){
		String s="And(";
		for(Op op:ops){
			s+=op+";";
		}
		s.substring(0, s.length()-1);
		s+=")";
		
		return s;
	}
}
class Or extends Op{
	HashSet<Op> ops;
	Or(HashSet<Op> ops){
		this.ops=ops;
	}
	boolean eval(Post p){
		for(Op op:ops){
			if(op.eval(p)){
				return true;
			}
		}
		return false;
	}
	public String toString(){
		String s="Or(";
		for(Op op:ops){
			s+=op+";";
		}
		s.substring(0, s.length()-1);
		s+=")";
		
		return s;
	}
}
class Not extends Op{
	Op op;
	Not(Op op){
		this.op=op;
	}
	boolean eval(Post p){
		if(op.eval(p)){
				return false;
		}
		return true;
	}
	public String toString(){
		String s="Not("+op.toString()+")";
		return s;
	}
}