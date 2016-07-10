package niki.tool;
/**

 ** Java Program to implement Sparse Vector

 **/

 

import java.util.Scanner;

import java.util.TreeMap;

import java.util.Map;

 

/** Class SparseVector **/

public class SparseVector

{

	/* Tree map is used to maintain sorted order */

    private TreeMap<Integer, Double> st;

   

 

    /** Constructor **/

    public SparseVector()

    {

       

        st = new TreeMap<Integer, Double>();

    }

    public void add (int i, double value) {
    	if (!st.containsKey(i))
    		st.put(i, 0.);
    	st.put(i, st.get(i) + value);
    	
    }

    /** Function to insert a (key, value) pair **/

    public void put(int i, double value) 

    {

 

        if (value == 0.0) 

            st.remove(i);

        else

            st.put(i, value);

    }

 

    /** Function to get value for a key **/

    public double get(int i) 

    {


        if (st.containsKey(i)) 

            return st.get(i);

        else                

            return 0.0;

    }

 

  
 

    /** Function to get dot product of two vectors **/

    public double dot(SparseVector b) 

    {

        SparseVector a = this;

      

        double sum = 0.0;

 

        if (a.st.size() <= b.st.size()) 

        {

            for (Map.Entry<Integer, Double> entry : a.st.entrySet())

                if (b.st.containsKey(entry.getKey()))

                    sum += a.get(entry.getKey()) * b.get(entry.getKey());

        }          

        else  

        {

            for (Map.Entry<Integer, Double> entry : b.st.entrySet())

                if (a.st.containsKey(entry.getKey()))

                    sum += a.get(entry.getKey()) * b.get(entry.getKey());

        }

        return sum;

    }

 

    /** Function to get sum of two vectors **/

    public SparseVector plus(SparseVector b) 

    {

        SparseVector a = this;

 

        SparseVector c = new SparseVector();

 

        for (Map.Entry<Integer, Double> entry : a.st.entrySet())

            c.put(entry.getKey(), a.get(entry.getKey()));

 

        for (Map.Entry<Integer, Double> entry : b.st.entrySet())

            c.put(entry.getKey(), b.get(entry.getKey()) + c.get(entry.getKey()));

 

        return c;

    }

 
    public double norm () {
    	return Math.sqrt(this.dot(this));
    }
    /** Function toString() for printing vector **/

    public String toString() 

    {

        String s = "";

        for (Map.Entry<Integer, Double> entry : st.entrySet())

            s += "(" + entry.getKey() + ", " + st.get(entry.getKey()) + ") ";

 

        return s;

    }

}