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
package org.sf.xrime.algorithms.setBFS;

import java.io.IOException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.sf.xrime.ProcessorExecutionException;
import org.sf.xrime.algorithms.GraphAlgorithm;
import org.sf.xrime.model.vertex.LabeledAdjSetVertex;
import org.sf.xrime.utils.SequenceTempDirMgr;


public class SetBFSStep extends GraphAlgorithm {  
  private String continueFileName="continue";
  static final public String continueFileKey="xrime.algorithm.BFS.continue.flag";
  private boolean end=false;

  private JobConf jobConf;  
  private FileSystem client=null;
  
  private SequenceTempDirMgr tempDirs=null;

  public String getContinueFlag() {
    return continueFileName;
  }

  public void setContinueFlag(String continueFlag) {
    this.continueFileName = continueFlag;
  }
  
  private String continueFlagFile() throws IllegalAccessException {
    Path filePath=new Path(context.getDestination().getPath().toString()+"/"+continueFileName);
      return filePath.toString();
  }
  
  public boolean isEnd() {
    return end;
  }  
  
  public FileSystem getClient() {
    return client;
  }

  public void setClient(FileSystem client) {
    this.client = client;
  }

  public SequenceTempDirMgr getTempDirs() {
    return tempDirs;
  }

  public void setTempDirs(SequenceTempDirMgr tempDirs) {
    this.tempDirs = tempDirs;
  }
  
  @Override
  public void execute() throws ProcessorExecutionException {
    try {
      context.setParameter(continueFileKey, continueFlagFile());
      
      jobConf = new JobConf(context, SetBFSStep.class);
      jobConf.setJobName("BFS");
  
      FileInputFormat.setInputPaths(jobConf, context.getSource().getPath());
        
      jobConf.setInputFormat(SequenceFileInputFormat.class);  
      jobConf.setMapperClass(SetBFSMapper.class);
      jobConf.setNumMapTasks(getMapperNum());        
      jobConf.setMapOutputValueClass(LabeledAdjSetVertex.class);    
        
      jobConf.setReducerClass(SetBFSReducer.class);
      jobConf.setNumReduceTasks(getReducerNum());
        
      jobConf.setOutputKeyClass(Text.class);
      jobConf.setOutputValueClass(LabeledAdjSetVertex.class);
      
      FileOutputFormat.setOutputPath(jobConf, context.getDestination().getPath());
      jobConf.setOutputFormat(SequenceFileOutputFormat.class);    
  
          
      this.runningJob = JobClient.runJob(jobConf);
      
      if(client==null) {
        client=FileSystem.get(jobConf);
      }
      if(client.exists(new Path(continueFlagFile()))) {
        end=false;
        client.delete(new Path(continueFlagFile()), true);
      }
      else {
        end=true;
      }  
    } catch (IOException e) {
      throw new ProcessorExecutionException(e);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }
}
