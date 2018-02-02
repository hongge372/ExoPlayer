/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.source.dash.offline;

import static com.google.android.exoplayer2.source.dash.offline.DashDownloadTestData.TEST_MPD;
import static com.google.android.exoplayer2.source.dash.offline.DashDownloadTestData.TEST_MPD_URI;
import static com.google.android.exoplayer2.testutil.CacheAsserts.assertCacheEmpty;
import static com.google.android.exoplayer2.testutil.CacheAsserts.assertCachedData;

import android.content.Context;
import android.content.Intent;
import android.test.InstrumentationTestCase;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper;
import com.google.android.exoplayer2.source.dash.manifest.RepresentationKey;
import com.google.android.exoplayer2.testutil.FakeDataSet;
import com.google.android.exoplayer2.testutil.FakeDataSource;
import com.google.android.exoplayer2.testutil.TestUtil;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.ConditionVariable;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.util.scheduler.Requirements;
import com.google.android.exoplayer2.util.scheduler.Scheduler;
import java.io.File;

/**
 * Unit tests for {@link DownloadService}.
 */
public class DownloadServiceDashTest extends InstrumentationTestCase {

  private SimpleCache cache;
  private File tempFolder;
  private FakeDataSet fakeDataSet;
  private RepresentationKey fakeRepresentationKey1;
  private RepresentationKey fakeRepresentationKey2;
  private Context context;
  private DownloadService dashDownloadService;
  private ConditionVariable pauseDownloadCondition;
  private TestDownloadListener testDownloadListener;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    tempFolder = Util.createTempDirectory(getInstrumentation().getContext(), "ExoPlayerTest");
    cache = new SimpleCache(tempFolder, new NoOpCacheEvictor());

    Runnable pauseAction = new Runnable() {
      @Override
      public void run() {
        if (pauseDownloadCondition != null) {
          try {
            pauseDownloadCondition.block();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    };
    fakeDataSet = new FakeDataSet()
        .setData(TEST_MPD_URI, TEST_MPD)
        .newData("audio_init_data")
        .appendReadAction(pauseAction)
        .appendReadData(TestUtil.buildTestData(10))
        .endData()
        .setRandomData("audio_segment_1", 4)
        .setRandomData("audio_segment_2", 5)
        .setRandomData("audio_segment_3", 6)
        .setRandomData("text_segment_1", 1)
        .setRandomData("text_segment_2", 2)
        .setRandomData("text_segment_3", 3);
    DataSource.Factory fakeDataSourceFactory = new FakeDataSource.Factory(null)
        .setFakeDataSet(fakeDataSet);
    fakeRepresentationKey1 = new RepresentationKey(0, 0, 0);
    fakeRepresentationKey2 = new RepresentationKey(0, 1, 0);

    context = getInstrumentation().getContext();

    File actionFile = Util.createTempFile(context, "ExoPlayerTest");
    actionFile.delete();
    final DownloadManager dashDownloadManager =
        new DownloadManager(
            new DownloaderConstructorHelper(cache, fakeDataSourceFactory),
            1,
            3,
            actionFile.getAbsolutePath(),
            DashDownloadAction.DESERIALIZER);
    testDownloadListener = new TestDownloadListener(dashDownloadManager, this);
    dashDownloadManager.addListener(testDownloadListener);
    dashDownloadManager.startDownloads();

    try {
      runTestOnUiThread(
          new Runnable() {
            @Override
            public void run() {
              dashDownloadService =
                  new DownloadService(101010) {

                    @Override
                    protected DownloadManager getDownloadManager() {
                      return dashDownloadManager;
                    }

                    @Override
                    protected String getNotificationChannelId() {
                      return null;
                    }

                    @Override
                    protected Scheduler getScheduler() {
                      return null;
                    }

                    @Override
                    protected Requirements getRequirements() {
                      return null;
                    }
                  };
              dashDownloadService.onCreate();
            }
          });
    } catch (Throwable throwable) {
      throw new Exception(throwable);
    }
  }

  @Override
  public void tearDown() throws Exception {
    try {
      runTestOnUiThread(new Runnable() {
          @Override
          public void run() {
            dashDownloadService.onDestroy();
          }
        });
    } catch (Throwable throwable) {
      throw new Exception(throwable);
    }
    Util.recursiveDelete(tempFolder);
    super.tearDown();
  }

  public void testMultipleDownloadAction() throws Throwable {
    downloadKeys(fakeRepresentationKey1);
    downloadKeys(fakeRepresentationKey2);

    testDownloadListener.blockUntilTasksCompleteAndThrowAnyDownloadError();

    assertCachedData(cache, fakeDataSet);
  }

  public void testRemoveAction() throws Throwable {
    downloadKeys(fakeRepresentationKey1, fakeRepresentationKey2);

    testDownloadListener.blockUntilTasksCompleteAndThrowAnyDownloadError();

    removeAll();

    testDownloadListener.blockUntilTasksCompleteAndThrowAnyDownloadError();

    assertCacheEmpty(cache);
  }

  public void testRemoveBeforeDownloadComplete() throws Throwable {
    pauseDownloadCondition = new ConditionVariable();
    downloadKeys(fakeRepresentationKey1, fakeRepresentationKey2);

    removeAll();

    testDownloadListener.blockUntilTasksCompleteAndThrowAnyDownloadError();

    assertCacheEmpty(cache);
  }

  private void removeAll() throws Throwable {
    callDownloadServiceOnStart(new DashDownloadAction(TEST_MPD_URI, true, null));
  }

  private void downloadKeys(RepresentationKey... keys) throws Throwable {
    callDownloadServiceOnStart(new DashDownloadAction(TEST_MPD_URI, false, null, keys));
  }

  private void callDownloadServiceOnStart(final DashDownloadAction action) throws Throwable {
    runTestOnUiThread(
        new Runnable() {
          @Override
          public void run() {
            Intent startIntent =
                DownloadService.createAddDownloadActionIntent(
                    context, DownloadService.class, action);
            dashDownloadService.onStartCommand(startIntent, 0, 0);
          }
        });
  }

}