/*
 * Copyright 2019-present Facebook, Inc.
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

package com.facebook.buck.parser.targetnode;

import com.facebook.buck.core.graph.transformation.model.ClassBasedComputationIdentifier;
import com.facebook.buck.core.graph.transformation.model.ComputationIdentifier;
import com.facebook.buck.core.graph.transformation.model.ComputeKey;
import com.facebook.buck.core.model.targetgraph.raw.UnconfiguredTargetNode;
import com.facebook.buck.core.model.targetgraph.raw.UnconfiguredTargetNodeWithDeps;
import java.nio.file.Path;
import org.immutables.value.Value;

/**
 * Transformation key containing {@link UnconfiguredTargetNode} to translate it to {@link
 * UnconfiguredTargetNodeWithDeps}.
 */
@Value.Immutable(builder = false, copy = false, prehash = true)
public abstract class UnconfiguredTargetNodeToUnconfiguredTargetNodeWithDepsKey
    implements ComputeKey<UnconfiguredTargetNodeWithDeps> {

  public static final ComputationIdentifier<UnconfiguredTargetNodeWithDeps> IDENTIFIER =
      ClassBasedComputationIdentifier.of(
          UnconfiguredTargetNodeToUnconfiguredTargetNodeWithDepsKey.class,
          UnconfiguredTargetNodeWithDeps.class);

  /**
   * {@link UnconfiguredTargetNode} which should be translated to {@link
   * UnconfiguredTargetNodeWithDeps}
   */
  @Value.Parameter
  public abstract UnconfiguredTargetNode getUnconfiguredTargetNode();

  /**
   * {@link Path} to the root of a package that has this {@link UnconfiguredTargetNode}, relative to
   * parse root, usually cell root
   */
  @Value.Parameter
  public abstract Path getPackagePath();

  @Override
  public ComputationIdentifier<UnconfiguredTargetNodeWithDeps> getIdentifier() {
    return IDENTIFIER;
  }
}
