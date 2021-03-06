/*
 * Copyright (C) yangyin@BUPT. 2009.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sf.xrime.preprocessing.pajek;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.DefaultCodec;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.ReflectionUtils;
import org.sf.xrime.algorithms.MST.MSTLabel.MSTWEdgesLabel;
import org.sf.xrime.model.edge.Edge;
import org.sf.xrime.model.edge.WeightOfEdge;
import org.sf.xrime.model.vertex.LabeledAdjVertex;


/**
 * This transformer would transform pajek data format to a format which could be processed by Pajek2AdjVertexTransformer.
 * @author Yang Cheng
 * The pajek data format is:
 * *Vertices 3 
 * 1 "Doc1" 0.0 0.0 0.0 ic Green bc Brown 
 * 2 "Doc2" 0.0 0.0 0.0 ic Green bc Brown 
 * 3 "Doc3" 0.0 0.0 0.0 ic Green bc Brown  
 * *Arcs 
 * 1 2 3 c Green 
 * 2 3 5 c Black 
 * *Edges 
 * 1 3 4 c Green 
 * this class deal with the raw data of pajek ,transform the data to following format, which could be processed by Map/reduce
 * v 1 "Doc1" 0.0 0.0 0.0 ic Green bc Brown
 * v 2 "Doc2" 0.0 0.0 0.0 ic Green bc Brown
 * v 3 "Doc3" 0.0 0.0 0.0 ic Green bc Brown 
 * a 1 2 3 c Green
 * a 2 3 5 c Black
 * e 1 3 4 c Green 
 *where marker (v,a,e) indicate the kind of input, v means a single vertex, a means arcs and e means edge
 * Modifer1 by yangyin, vertexes with weighted edges feature has been added,
 * besides, problem with vertex id has been fixed
 */
public class PajekFormat2WeightedLabeledAdjVertex {
	
	Map<String, LabeledAdjVertex> vertexes;
	String srcPath;
	String dstPath;
	
	public String getSrcPath() {
		return srcPath;
	}


	public void setSrcPath(String srcPath) {
		this.srcPath = srcPath;
	}

	public String getDestPath() {
		return dstPath;
	}

	public void setDestPath(String destPath) {
		this.dstPath = destPath;
	}

	public PajekFormat2WeightedLabeledAdjVertex(){
		vertexes = new HashMap<String, LabeledAdjVertex>();
	}
	public PajekFormat2WeightedLabeledAdjVertex(String srcPath, String destPath){
		this();
		this.srcPath = srcPath;
		this.dstPath = destPath;
	}
	
	public void addEdge(String from, String to, String weight){
				
		LabeledAdjVertex vertexFrom = vertexes.get(from);
		LabeledAdjVertex vertexTo = vertexes.get(to);
		//System.out.println("d:" + from);
		vertexFrom.addEdge(new Edge(vertexFrom.getId(), vertexTo.getId()));
		//System.out.println(currentVertex.getEdges().size());
		MSTWEdgesLabel mstWEdgesLabel = (MSTWEdgesLabel)vertexFrom.getLabel(MSTWEdgesLabel.mstWEdgesLabel);
		mstWEdgesLabel.addWEdge(vertexTo.getId(), new WeightOfEdge(Double.parseDouble(weight)));
	}
	
	public void readPajekFile(){
		String currentLine = "";
		String vertexId = "";
		String edgeInfor[] = new String[4];
		LabeledAdjVertex currentVertex = new LabeledAdjVertex();
		MSTWEdgesLabel mstWEdgesLabel = new MSTWEdgesLabel();
		int verNum = 0;
		vertexes.clear();
		File input = new File(srcPath);
		try{
			FileReader fr = new FileReader(input);
			BufferedReader br = new BufferedReader(fr);
			//create vertexes
			currentLine = br.readLine();
			while(!currentLine.contains((CharSequence)"Vertices"))
				currentLine = br.readLine();
			verNum=Integer.parseInt(currentLine.split("\\s+")[1].trim());
			for(int i = 0;i < verNum; i++){
				currentLine = br.readLine();
				String mapKey = "";
				if(currentLine.contains((CharSequence)("\""))){
					vertexId = currentLine.split("\"")[1];
					mapKey = currentLine.split("\"")[0];
				}
				else{
					vertexId = currentLine.split(" ")[0].trim();
					mapKey = vertexId;
				}
				if(vertexes.containsKey(vertexId))
					throw new WrongPajekFileFormatException("Vertexe id is not unique");
				
				currentVertex = new LabeledAdjVertex();
				currentVertex.setId(vertexId.trim());
				mstWEdgesLabel = new MSTWEdgesLabel();
				currentVertex.setLabel(MSTWEdgesLabel.mstWEdgesLabel, mstWEdgesLabel);
				vertexes.put(mapKey.trim(), currentVertex);
			}
			//process arcs
			currentLine = br.readLine();
			while(!currentLine.contains((CharSequence)"Arcs"))
				currentLine=br.readLine();
			currentLine = br.readLine();
			while(!currentLine.contains((CharSequence)"Edges")){
				edgeInfor = currentLine.split("\\s+");
				addEdge(edgeInfor[0].trim(), edgeInfor[1].trim(), edgeInfor[2].trim());
				currentLine = br.readLine();
			}
			//process edges
			currentLine = br.readLine();
			while(currentLine != null && !currentLine.equals("")){
				edgeInfor = currentLine.split("\\s+");
				addEdge(edgeInfor[0].trim(), edgeInfor[1].trim(), edgeInfor[2].trim());
				addEdge(edgeInfor[1].trim(), edgeInfor[0].trim(), edgeInfor[2].trim());
				currentLine = br.readLine();
			}
			Set<String> keySet = vertexes.keySet();
			Iterator<String> iter = keySet.iterator();
			while(iter.hasNext()){
				String currentVertexId = (String)iter.next();
				currentVertex = vertexes.get(currentVertexId);
				System.out.println(currentVertex.toString());
			}
		}catch(FileNotFoundException fileNotFoundException){
			
		}catch(IOException ioException){
			
		}catch(WrongPajekFileFormatException wrongPajekFileFormatExcetpion){
			
		}
	}
	
	public void toBinaryData(){
		try{
			JobConf jobConf = new JobConf(new Configuration(), PajekFormat2WeightedLabeledAdjVertex.class);
			
			Path filePath = new Path(dstPath + "/part00000");
	    	Path path = new Path(jobConf.getWorkingDirectory(), filePath);
			FileSystem fs = path.getFileSystem(jobConf);
	    	
			CompressionCodec codec = null;
			CompressionType compressionType = CompressionType.NONE;
			if (jobConf.getBoolean("mapred.output.compress", false)) {
				// find the kind of compression to do	          
				String val = jobConf.get("mapred.output.compression.type", CompressionType.RECORD.toString());
				compressionType=CompressionType.valueOf(val);
	
				// find the right codec
				Class<? extends CompressionCodec> codecClass = DefaultCodec.class;
				String name = jobConf.get("mapred.output.compression.codec");
				if (name != null) {
					try {
						codecClass = jobConf.getClassByName(name).asSubclass(CompressionCodec.class);
					} catch (ClassNotFoundException e) {
						throw new IllegalArgumentException("Compression codec " + name + 
								" was not found.", e);
					}
				}
				codec = ReflectionUtils.newInstance(codecClass, jobConf);
			}
			
			Set<String> keySet = vertexes.keySet();
			Iterator<String> iter = keySet.iterator();
			LabeledAdjVertex currentAdjVertex = new LabeledAdjVertex();
		
			SequenceFile.Writer out = SequenceFile.createWriter(fs, jobConf, path,
					Text.class,
					LabeledAdjVertex.class,
					compressionType,
					codec,
					null);
			
			while(iter.hasNext()){
				currentAdjVertex = vertexes.get(iter.next());
				out.append(new Text(currentAdjVertex.getId()), currentAdjVertex);
			}
			out.close();
		}catch(IOException e){
			
		}
		
	
	}
	
	public static void test()
	{
		//PajekFormat2WeightedLabeledAdjVertex pft = new PajekFormat2WeightedLabeledAdjVertex
		//	("/home/yangyin/development/hadoop-0.19.1/workspace/MST/Pajek_MST_Input/Pajek_MST_Input.NET", 
		//			"/home/yangyin/development/hadoop-0.19.1/workspace/MST/Input");
		PajekFormat2WeightedLabeledAdjVertex pft = new PajekFormat2WeightedLabeledAdjVertex
			("/home/yangyin/development/hadoop-0.19.1/workspace/MST/Pajek_MST_Input/Pajek_MST_Input.NET", 
					"/home/yangyin/development/hadoop-0.19.1/workspace/MST/Input");
		pft.readPajekFile();
		pft.toBinaryData();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//PajekFormat2WeightedLabeledAdjVertex.test();
		//System.exit(1);
		
		if(args.length!=2)
		{
			System.err.println("usage: <in> <out>");
			System.exit(1);
		}        
		PajekFormat2WeightedLabeledAdjVertex pft = new PajekFormat2WeightedLabeledAdjVertex(args[0], args[1]);
		pft.readPajekFile();
		pft.toBinaryData();
	}
}
