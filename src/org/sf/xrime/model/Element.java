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
package org.sf.xrime.model;

import java.util.Iterator;

import org.apache.hadoop.io.Writable;

/**
 * An interface for elements (vertices and edges) of generalized graphs.
 * Allows code to be written that applies to both vertices and edges, when their
 * structural role in a graph is not relevant (such as decorations).
 * This is useful in Map/Reduce framework.
 */
public interface Element extends Writable {
	/**
	 * Get the iterator to those incident elements of this element. For a vertex,
	 * the incident elements are usually edges (may be incoming, outgoing, both, or other
	 * combinations, this depends on the usage of the derivative class). For an 
	 * edge, the incident elements are usually vertexes (both ends of the edge).
	 * @return the iterator
	 */
	public Iterator<? extends Element> getIncidentElements();
	/**
	 * To accommodate the Text way of HadoopML, we add this method. It should be noted that,
	 * Text format is both inconvenient and inefficient for X-RIME. And, we assume the string
	 * input is generated by Element.toString(). So, each subclass of Element need to implement
	 * toString() and fromString together in its own way, and only a few validation is executed.
	 * 
	 * NOTE: Class hierarchy information may be lost in toString() and fromString(), in contrast
	 * to readFields() and write(). As a natural result, may have limitations on leveraging the
	 * class hierarchy.
	 * @param encoding
	 */
	public void fromString (String encoding);
}
