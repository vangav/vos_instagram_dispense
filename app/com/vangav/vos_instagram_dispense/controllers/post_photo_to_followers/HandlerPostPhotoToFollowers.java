/**
 * "First, solve the problem. Then, write the code. -John Johnson"
 * "Or use Vangav M"
 * www.vangav.com
 * */

/**
 * MIT License
 *
 * Copyright (c) 2016 Vangav
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 * */

/**
 * Community
 * Facebook Group: Vangav Open Source - Backend
 *   fb.com/groups/575834775932682/
 * Facebook Page: Vangav
 *   fb.com/vangav.f
 * 
 * Third party communities for Vangav Backend
 *   - play framework
 *   - cassandra
 *   - datastax
 *   
 * Tag your question online (e.g.: stack overflow, etc ...) with
 *   #vangav_backend
 *   to easier find questions/answers online
 * */

package com.vangav.vos_instagram_dispense.controllers.post_photo_to_followers;

import java.util.Calendar;
import java.util.UUID;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.BatchStatement.Type;
import com.vangav.backend.cassandra.Cassandra;
import com.vangav.backend.cassandra.formatting.CalendarFormatterInl;
import com.vangav.backend.metrics.time.CalendarAndDateOperationsInl;
import com.vangav.backend.play_framework.request.Request;
import com.vangav.backend.play_framework.request.RequestJsonBody;
import com.vangav.backend.play_framework.request.response.ResponseBody;
import com.vangav.vos_instagram_dispense.cassandra_keyspaces.ig_app_data.Follower;
import com.vangav.vos_instagram_dispense.cassandra_keyspaces.ig_app_data.UserFeedPosts;
import com.vangav.vos_instagram_dispense.cassandra_keyspaces.ig_jobs.CurrentJobs;
import com.vangav.vos_instagram_dispense.cassandra_keyspaces.ig_jobs.HourlyCurrentJobs;
import com.vangav.vos_instagram_dispense.controllers.CommonPlayHandler;

/**
 * GENERATED using ControllersGeneratorMain.java
 */
/**
 * HandlerPostPhotoToFollowers
 *   handles request-to-response processing
 *   also handles after response processing (if any)
 * */
public class HandlerPostPhotoToFollowers extends CommonPlayHandler {

  private static final String kName = "PostPhotoToFollowers";

  @Override
  protected String getName () {

    return kName;
  }

  @Override
  protected RequestJsonBody getRequestJson () {

    return new RequestPostPhotoToFollowers();
  }

  @Override
  protected ResponseBody getResponseBody () {

    return new ResponsePostPhotoToFollowers();
  }

  @Override
  protected void processRequest (final Request request) throws Exception {

    // return right away to free resources on the main service side
    
    // all processing happens in after processing <afterProcessing>
    
    // vos_instagram_jobs takes care of retrying failed jobs
  }
  
  private static final int kCassandraPrefetchLimit = 100;

  @Override
  protected void afterProcessing (
    final Request request) throws Exception {
    
    // get request's body
    RequestPostPhotoToFollowers requestPostPhotoToFollowers =
      (RequestPostPhotoToFollowers)request.getRequestJsonBody();
    
    // get user's followers
    ResultSet resultSet =
      Follower.i().executeSyncSelectAll(
        requestPostPhotoToFollowers.getUserId() );
    
    // dispense new post to each follower
    for (Row row : resultSet) {
      
      if (resultSet.getAvailableWithoutFetching() <=
          kCassandraPrefetchLimit &&
          resultSet.isFullyFetched() == false) {
        
        // this is asynchronous
        resultSet.fetchMoreResults();
      }
      
      UserFeedPosts.i().executeSyncInsert(
        row.getUUID(Follower.kFollowerUserIdColumnName),
        requestPostPhotoToFollowers.post_id,
        UUID.fromString(requestPostPhotoToFollowers.post_id) );
    }

    // finished processing successfully - delete job
    
    UUID jobId = UUID.fromString(requestPostPhotoToFollowers.job_id);
    
    // all queries must succeed
    BatchStatement batchStatement = new BatchStatement(Type.LOGGED);
    
    batchStatement.add(
      CurrentJobs.i().getBoundStatementDelete(jobId) );
    
    batchStatement.add(
      HourlyCurrentJobs.i().getBoundStatementDelete(
        CalendarFormatterInl.concatCalendarFields(
          CalendarAndDateOperationsInl.getCalendarFromUnixTime(
            requestPostPhotoToFollowers.post_time),
          Calendar.YEAR,
          Calendar.MONTH,
          Calendar.DAY_OF_MONTH,
          Calendar.HOUR_OF_DAY),
        requestPostPhotoToFollowers.post_time,
        jobId) );
    
    // execute batch statement
    Cassandra.i().executeSync(batchStatement);
  }
}
