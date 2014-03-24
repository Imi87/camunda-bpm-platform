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

package org.camunda.bpm.engine.test.concurrency;

import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.test.Deployment;

import java.util.HashMap;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


/**
 * @author Tom Baeyens
 */
public class CompetingMessagesTest extends PluggableProcessEngineTestCase {

  private static Logger log = Logger.getLogger(CompetingMessagesTest.class.getName());

  private static final int NUMBER_OF_THREADS = 3;
  public static final int NUM_OF_PROCESS_INSTANCES = 30;

  CountDownLatch latch = new CountDownLatch(NUM_OF_PROCESS_INSTANCES);
  Queue<String> correlationIds = new ConcurrentLinkedQueue<String>();

  public class StarterThread implements Runnable {

    Exception exception;

    public void run() {
      try {
        HashMap<String, Object> variables = new HashMap<String, Object>();
        String correlateId = UUID.randomUUID().toString();
        correlationIds.add(correlateId);
        variables.put("correlation", correlateId);
        runtimeService.startProcessInstanceByKey("competingMessages", variables);
      } catch (Exception e) {
        this.exception = e;
      } finally {
        synchronized (latch) {
          latch.countDown();
        }
        log.fine(StarterThread.class.getName() + " ends");
      }
    }
  }

  public class MessageThread implements Runnable {

    Exception exception;

    public void run() {
      try {
        String correlationId = correlationIds.poll();

        if (correlationId != null) {
          HashMap<String, Object> variables = new HashMap<String, Object>();
          variables.put("result", Boolean.toString(correlationId.hashCode() % 2 == 0));

          HashMap<String, Object> correlationKeys = new HashMap<String, Object>();
          correlationKeys.put("correlation", correlationId);
          runtimeService.correlateMessage("catch-msg", correlationKeys, variables);
        }
      } catch (Exception e) {
        this.exception = e;
        Thread.currentThread().interrupt();
      } finally {
        synchronized (latch) {
          latch.countDown();
        }
        log.fine(MessageThread.class.getName() + " ends");
      }
    }
  }
  
  @Deployment
  public void testCompetingCorrelatingMessages() throws Exception {
    final ExecutorService taskExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    final StarterThread prozessStarter = new StarterThread();
    for (int i = 0; i < NUM_OF_PROCESS_INSTANCES; i++) {
      taskExecutor.execute(prozessStarter);
    }
    latch.await();

    final MessageThread messageSender = new MessageThread();
    for (int i = 0; i < NUM_OF_PROCESS_INSTANCES; i++) {
      taskExecutor.execute(messageSender);
    }
    latch.await();

    taskExecutor.shutdown();

    assertNull(prozessStarter.exception);
    assertNotNull(messageSender.exception);
  }
}
