/*
 * Copyright (C) IBM Corp. 2009.
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
package org.sf.xrime.algorithms.partitions.connected.weakly.alg_1;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.sf.xrime.ProcessorExecutionException;
import org.sf.xrime.algorithms.GraphAlgorithm;
import org.sf.xrime.algorithms.utils.GraphAlgorithmMapReduceBase;
import org.sf.xrime.model.Graph;
import org.sf.xrime.model.vertex.VertexSet;


/**
 * Remove redundant <vertex, label> pairs during the backward label propagation process. 
 * @author xue
 */
public class VertexSetMinorExpand extends GraphAlgorithm {
  /**
   * Default constructor.
   */
  public VertexSetMinorExpand(){
    super();
  }
  /**
   * Emit without change.
   * @author xue
   */
  public static class MapClass extends GraphAlgorithmMapReduceBase implements
    Mapper<Text, VertexSet, Text, VertexSet>{

    @Override
    public void map(Text key, VertexSet value,
        OutputCollector<Text, VertexSet> output, Reporter reporter)
        throws IOException {
      output.collect(key, value);
    }
  }
  
  /**
   * Remove redundant pairs.
   * @author xue
   */
  public static class ReduceClass extends GraphAlgorithmMapReduceBase implements
    Reducer<Text, VertexSet, Text, VertexSet>{

    @Override
    public void reduce(Text key, Iterator<VertexSet> values,
        OutputCollector<Text, VertexSet> output, Reporter reporter)
        throws IOException {
      output.collect(key, values.next());
    }
  }
  
  @Override
  public void setArguments(String[] params) throws ProcessorExecutionException {
    // Make sure there are exactly 2 parameters left.
    if (params.length != 2) {
      throw new ProcessorExecutionException("Wrong number of parameters: " +
                         params.length + " instead of 2.");
    }
    
    // Configure the algorithm instance.
    Graph src = new Graph(Graph.defaultGraph());
    src.setPath(new Path(params[0]));
    Graph dest = new Graph(Graph.defaultGraph());
    dest.setPath(new Path(params[1]));
    
    setSource(src);
    setDestination(dest);
  }

  @Override
  public void execute() throws ProcessorExecutionException {
    JobConf conf = new JobConf(context, VertexSetMinorExpand.class);
    conf.setJobName("VertexSetMinorExpand");
 
    // the keys are vertex identifiers (strings)
    conf.setOutputKeyClass(Text.class);
    // the values are ultimate vertex set identifiers/label (Writable)
    conf.setOutputValueClass(VertexSet.class);
    conf.setMapperClass(MapClass.class);        
    conf.setCombinerClass(ReduceClass.class);        
    conf.setReducerClass(ReduceClass.class);
    // makes the file format suitable for machine processing.
    conf.setInputFormat(SequenceFileInputFormat.class);
    conf.setOutputFormat(SequenceFileOutputFormat.class);
    try {
      FileInputFormat.setInputPaths(conf, getSource().getPath());
      FileOutputFormat.setOutputPath(conf, getDestination().getPath());
    } catch (IllegalAccessException e1) {
      throw new ProcessorExecutionException(e1);
    }
    conf.setNumMapTasks(getMapperNum());
    conf.setNumReduceTasks(getReducerNum());
        
    try {
      this.runningJob = JobClient.runJob(conf);
    } catch (IOException e) {
      throw new ProcessorExecutionException(e);
    }
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      int res = ToolRunner.run(new VertexSetMinorExpand(), args);
      System.exit(res);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
