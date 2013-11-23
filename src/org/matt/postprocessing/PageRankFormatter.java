package org.matt.postprocessing;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.sf.xrime.ProcessorExecutionException;
import org.sf.xrime.Transformer;
import org.sf.xrime.postprocessing.SequenceFileToTextFileMapper;
import org.sf.xrime.postprocessing.SequenceFileToTextFileTransformer;


/**
 * This class is used to transform all graph algorithms' outputs in form of sequence files
 * into text files, which are human-readable.
 */
public class PageRankFormatter extends Transformer{
  /**
   * Default constructor.
   */
  public PageRankFormatter(){
    super();
  }
  /**
   * Constructor.
   * @param src
   * @param dest
   */
	public PageRankFormatter(Path src, Path dest) {
	  super(src, dest);
	}

  @Override
  public void execute() throws ProcessorExecutionException {
    JobConf convertor =  new JobConf(conf, PageRankFormatter.class);
    convertor.setJobName("SequencePageRankToTabbedFormat");

    convertor.setInputFormat(SequenceFileInputFormat.class);
    convertor.setOutputFormat(TextOutputFormat.class);
    
	convertor.setMapperClass(PageRankFormatterMapper.class);
	convertor.setMapOutputValueClass(Text.class);
	convertor.setMapOutputKeyClass(Text.class);
	
    convertor.setOutputKeyClass(Text.class);
    convertor.setOutputValueClass(Text.class);
        
    // ONLY mapper, no combiner, no reducer.
    convertor.setNumMapTasks(getMapperNum());
	convertor.setNumReduceTasks(0);

    FileInputFormat.setInputPaths(convertor, srcPath);
    FileOutputFormat.setOutputPath(convertor, destPath);		   
        
    try {
      this.runningJob = JobClient.runJob(convertor);
      runningJob.waitForCompletion();
    } catch (IOException e) {
      throw new ProcessorExecutionException(e);
    }
  }
}