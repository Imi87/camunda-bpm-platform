/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.qa.performance.engine.framework.aggregate;

import java.io.File;

import org.camunda.bpm.qa.performance.engine.framework.PerfTestException;
import org.camunda.bpm.qa.performance.engine.framework.PerfTestResults;
import org.camunda.bpm.qa.performance.engine.util.JsonUtil;

/**
 * A result aggregator is used to aggregate the results of a
 * performance testsuite run as a table.
 *
 * The aggegator needs to be pointed to a directory containing the
 * result files. It will read the result file by file and delegate the
 * actual processing to a subclass implementation of this class.
 *
 * @author Daniel Meyer
 *
 */
public abstract class TabularResultAggregator {

  protected File resultDirectory;

  public TabularResultAggregator(String resultsFolderPath) {
    resultDirectory = new File(resultsFolderPath);
    if(!resultDirectory.exists()) {
      throw new PerfTestException("Folder "+resultsFolderPath+ " does not exist.");
    }
  }

  public TabularResultSet execute() {
    TabularResultSet tabularResultSet = createAggrgatedResultsInstance();

    File[] resultFiles = resultDirectory.listFiles();
    for (File resultFile : resultFiles) {
      if(resultFile.getName().endsWith(".json")) {
        processFile(resultFile, tabularResultSet);
      }
    }

    return tabularResultSet;
  }

  protected void processFile(File resultFile, TabularResultSet tabularResultSet) {

    PerfTestResults results = JsonUtil.readObjectFromFile(resultFile.getAbsolutePath(), PerfTestResults.class);
    processResults(results, tabularResultSet);

  }

  protected abstract TabularResultSet createAggrgatedResultsInstance();

  protected abstract void processResults(PerfTestResults results, TabularResultSet tabularResultSet);

}