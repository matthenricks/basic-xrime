package org.matt.postprocessing;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.sf.xrime.algorithms.pagerank.PageRankLabel;
import org.sf.xrime.model.vertex.LabeledAdjSetVertex;


/***
 * Class built specifically to put the PageRank final outputs within a Tabbed format
 * @author cloudera
 *
 */
public class PageRankFormatterMapper extends MapReduceBase
implements Mapper<Text, LabeledAdjSetVertex, Text, Text> {
	
	@Override
	public void map(Text key, LabeledAdjSetVertex value,
		      OutputCollector<Text, Text> collector, Reporter reporter)
		      throws IOException {
		
		PageRankLabel label=(PageRankLabel) value.getLabel(PageRankLabel.pageRankLabelKey);
	    if(label==null || label.isReachable() == false) {
	      return;
	    }
	        
	    collector.collect(key, new Text(String.valueOf(label.getPr())));
	}

}