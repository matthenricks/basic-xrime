package org.matt.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;

/**
 * The objective of this class is to read in two text-based sequence outputs and compare them together -- up until they are too gigantic
 * Sadly, that would take some Hadoop ingenuity (subtracting like values?!?) but I'm not going to spend time on that yet :D
 * @author cloudera
 *
 */
public class HashFileComparison {

	/**
	 * A function that returns the paths of the parts of a folder or the file in a list
	 * @param folderOrFile
	 * @return
	 * @throws IOException 
	 */
	protected static Path[] globInternal(Path folderOrFile, FileSystem fs) throws IOException {
		Path input = folderOrFile;
		Path[] files;
		
		if (fs != null && fs.exists(input)) {
			if (fs.isDirectory(input)) {
				input = new Path(input.toUri().toString() + "/part-*");
				FileStatus[] fStatus= fs.globStatus(input);
				files = FileUtil.stat2Paths(fStatus);
			} else {
				Path[] temp = {input};
				files = temp;
			}
			return files;
		} else
			return null;
	}
	
	/**
	 * Function to read in data from a file in the following structure: <node> <double> \n
	 * and place it in a HashMap
	 * @param p
	 * @param myMap
	 * @param fs
	 * @throws IOException
	 */
	protected static void readIn(Path p, HashMap<String, Double> myMap, FileSystem fs) throws IOException {
	    FSDataInputStream inputStream = fs.open(p);
	    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
	    // Read in all the lines and push into hashmap
	    String line;
	    while((line = br.readLine()) != null) {
	    	String[] sectors = line.trim().split("\t");
	    	if (sectors.length != 2)
	    		throw new Error("Parsing of file does not match specified input -- readIn");
	    	if (myMap.containsKey(sectors[0]))
	    		throw new Error("Node is trying to be added more than once");
	    	
	    	myMap.put(sectors[0], Double.parseDouble(sectors[1]));
	    }
	    br.close();
	    fs.close();
	}
	
	/**
	 * Function to read in data from a file in the following structure: <node> <double> \n and compare it to the inside of a current hash
	 * @param p
	 * @param myMap
	 * @param fs
	 * @param tolerance
	 * @throws IOException
	 */
	protected static void compareIn(Path p, HashMap<String, Double> myMap, FileSystem fs, Double tolerance) throws IOException {
	    FSDataInputStream inputStream = fs.open(p);
	    BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
	    // Read in all the lines and push into hash-map
	    String line;
	    while((line = br.readLine()) != null) {
	    	String[] sectors = line.trim().split("\t");
	    	if (sectors.length != 2)
	    		throw new Error("Parsing of file does not match specified input -- readIn");
	    	if (myMap.containsKey(sectors[0]) == false)
	    		throw new Error("Node doesn't exist that is said ot exist: " + sectors[0]);
	    	
	    	if (Math.abs(myMap.get(sectors[0]).compareTo(Double.parseDouble(sectors[1]))) < tolerance) {
	    		myMap.remove(sectors[0]);
	    	} else {
	    		throw new Error("One of the keys has two very different values: {" + sectors[0] + "| val1: " + myMap.get(sectors[0]) + ", val2: " + sectors[1]);
	    	}
	    }
	    br.close();
	    fs.close();
	}
	
	/**
	 * Main runner of this comparator
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 2)
			throw new Error("Not enough arguments: bin 1 and bin 2 will be compared");
		
		Configuration conf = new Configuration();
		Path binLoc = new Path(args[0]);
		FileSystem fs = binLoc.getFileSystem(conf);
		// First import in bin 1
		// Check if it's a folder, file, or nothing
		Path[] bin = globInternal(binLoc, fs);
		if (bin == null)
			throw new Error("Input File Doesn't Exist");
		
		HashMap<String, Double> comparison_bin = new HashMap<String, Double>();
		// Now read in all the files in the bucket
		for (Path p : bin) {
			readIn(p, comparison_bin, fs);
		}
		
		// Now close the filesystem
		fs.close();
		
		// Then, import in bin 2, checking if they are indeed what's in the other fs
		binLoc = new Path(args[1]);
		fs = binLoc.getFileSystem(conf);
		bin = globInternal(binLoc, fs);
		
		// Now pull in the other and compare them
		for (Path p : bin) {
			compareIn(p, comparison_bin, fs, 0.001);
		}
		
		fs.close();
		
		if (!comparison_bin.isEmpty())
			throw new Error("Nodes were inadequitely addressed");
		
		System.exit(0);
	}
}
