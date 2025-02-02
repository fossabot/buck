/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.artifact_cache;

import com.facebook.buck.artifact_cache.config.ArtifactCacheMode;
import com.facebook.buck.artifact_cache.config.CacheReadMode;
import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.rulekey.RuleKey;
import com.facebook.buck.core.util.log.Logger;
import com.facebook.buck.event.BuckEventBus;
import com.facebook.buck.event.ConsoleEvent;
import com.facebook.buck.io.file.BorrowablePath;
import com.facebook.buck.io.file.LazyPath;
import com.facebook.buck.slb.NoHealthyServersException;
import com.facebook.buck.util.types.Pair;
import com.facebook.buck.util.types.Unit;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class RetryingCacheDecorator implements ArtifactCache, CacheDecorator {

  private static final Logger LOG = Logger.get(RetryingCacheDecorator.class);

  private final ArtifactCache delegate;
  private final int maxFetchRetries;
  private final BuckEventBus buckEventBus;
  private final ArtifactCacheMode cacheMode;

  public RetryingCacheDecorator(
      ArtifactCacheMode cacheMode,
      ArtifactCache delegate,
      int maxFetchRetries,
      BuckEventBus buckEventBus) {
    Preconditions.checkArgument(maxFetchRetries > 0);

    this.cacheMode = cacheMode;
    this.delegate = delegate;
    this.maxFetchRetries = maxFetchRetries;
    this.buckEventBus = buckEventBus;
  }

  @Override
  public ListenableFuture<CacheResult> fetchAsync(
      @Nullable BuildTarget target, RuleKey ruleKey, LazyPath output) {
    List<String> allCacheErrors = new ArrayList<>();
    ListenableFuture<CacheResult> resultFuture = delegate.fetchAsync(target, ruleKey, output);
    for (int retryCount = 1; retryCount < maxFetchRetries; retryCount++) {
      int retryCountForLambda = retryCount;
      resultFuture =
          Futures.transformAsync(
              resultFuture,
              result -> {
                if (result.getType() != CacheResultType.ERROR) {
                  return Futures.immediateFuture(result);
                }
                result.cacheError().ifPresent(allCacheErrors::add);
                LOG.info(
                    "Failed to fetch %s after %d/%d attempts, exception: %s",
                    ruleKey, retryCountForLambda + 1, maxFetchRetries, result.cacheError());
                return delegate.fetchAsync(target, ruleKey, output);
              });
    }
    return Futures.transform(
        resultFuture,
        result -> {
          if (result.getType() != CacheResultType.ERROR) {
            return result;
          }
          String msg = String.join("\n", allCacheErrors);
          if (!msg.contains(NoHealthyServersException.class.getName())) {
            buckEventBus.post(
                ConsoleEvent.warning(
                    "Failed to fetch %s over %s after %d attempts.",
                    ruleKey, cacheMode.name(), maxFetchRetries));
          }
          return CacheResult.builder().from(result).setCacheError(msg).build();
        },
        MoreExecutors.directExecutor());
  }

  @Override
  public void skipPendingAndFutureAsyncFetches() {
    delegate.skipPendingAndFutureAsyncFetches();
  }

  @Override
  public ArtifactCache getDelegate() {
    return delegate;
  }

  @Override
  public ListenableFuture<Unit> store(ArtifactInfo info, BorrowablePath output) {
    return delegate.store(info, output);
  }

  @Override
  public ListenableFuture<Unit> store(ImmutableList<Pair<ArtifactInfo, BorrowablePath>> artifacts) {
    return delegate.store(artifacts);
  }

  @Override
  public ListenableFuture<ImmutableMap<RuleKey, CacheResult>> multiContainsAsync(
      ImmutableSet<RuleKey> ruleKeys) {
    // Contains is best-effort.
    return delegate.multiContainsAsync(ruleKeys);
  }

  @Override
  public ListenableFuture<CacheDeleteResult> deleteAsync(List<RuleKey> ruleKeys) {
    return delegate.deleteAsync(ruleKeys);
  }

  @Override
  public CacheReadMode getCacheReadMode() {
    return delegate.getCacheReadMode();
  }

  @Override
  public void close() {
    delegate.close();
  }
}
