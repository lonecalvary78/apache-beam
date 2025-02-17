/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.runners.jet.metrics;

import java.util.Arrays;
import org.apache.beam.runners.core.metrics.BoundedTrieData;
import org.apache.beam.sdk.metrics.BoundedTrie;
import org.apache.beam.sdk.metrics.MetricName;

/** Implementation of {@link BoundedTrie}. */
public class BoundedTrieImpl extends AbstractMetric<BoundedTrieData> implements BoundedTrie {

  private final BoundedTrieData boundedTrieDataData = new BoundedTrieData();

  public BoundedTrieImpl(MetricName name) {
    super(name);
  }

  @Override
  BoundedTrieData getValue() {
    return boundedTrieDataData;
  }

  @Override
  public void add(Iterable<String> values) {
    boundedTrieDataData.add(values);
  }

  @Override
  public void add(String... values) {
    add(Arrays.asList(values));
  }
}
