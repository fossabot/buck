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

import com.facebook.buck.core.cell.Cell;
import com.facebook.buck.core.exceptions.DependencyStack;
import com.facebook.buck.core.graph.transformation.ComputationEnvironment;
import com.facebook.buck.core.graph.transformation.GraphComputation;
import com.facebook.buck.core.graph.transformation.model.ComputationIdentifier;
import com.facebook.buck.core.graph.transformation.model.ComputeKey;
import com.facebook.buck.core.graph.transformation.model.ComputeResult;
import com.facebook.buck.core.model.UnconfiguredBuildTarget;
import com.facebook.buck.core.model.UnconfiguredBuildTargetView;
import com.facebook.buck.core.model.impl.ImmutableUnconfiguredBuildTargetView;
import com.facebook.buck.core.model.targetgraph.raw.UnconfiguredTargetNode;
import com.facebook.buck.parser.UnconfiguredTargetNodeFactory;
import com.facebook.buck.parser.api.BuildFileManifest;
import com.facebook.buck.parser.config.ParserConfig;
import com.facebook.buck.parser.exceptions.NoSuchBuildTargetException;
import com.facebook.buck.parser.manifest.BuildPackagePathToBuildFileManifestKey;
import com.facebook.buck.parser.manifest.ImmutableBuildPackagePathToBuildFileManifestKey;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Transforms one target from specific {@link BuildFileManifest} to {@link UnconfiguredTargetNode}
 */
public class BuildTargetToUnconfiguredTargetNodeComputation
    implements GraphComputation<BuildTargetToUnconfiguredTargetNodeKey, UnconfiguredTargetNode> {

  private final UnconfiguredTargetNodeFactory unconfiguredTargetNodeFactory;
  private final Cell cell;
  private final Path buildFileName;

  private BuildTargetToUnconfiguredTargetNodeComputation(
      UnconfiguredTargetNodeFactory unconfiguredTargetNodeFactory, Cell cell) {
    this.unconfiguredTargetNodeFactory = unconfiguredTargetNodeFactory;
    this.cell = cell;
    buildFileName =
        cell.getRoot()
            .getFileSystem()
            .getPath(cell.getBuckConfigView(ParserConfig.class).getBuildFileName());
  }

  /**
   * Create new instance of {@link BuildTargetToUnconfiguredTargetNodeComputation}
   *
   * @param unconfiguredTargetNodeFactory An actual factory that will create {@link
   *     UnconfiguredTargetNode} from raw attributes containing in {@link BuildFileManifest}
   * @param cell A {@link Cell} object that contains targets used in this transformation, it is
   *     mostly used to resolve paths to absolute paths
   * @return
   */
  public static BuildTargetToUnconfiguredTargetNodeComputation of(
      UnconfiguredTargetNodeFactory unconfiguredTargetNodeFactory, Cell cell) {
    return new BuildTargetToUnconfiguredTargetNodeComputation(unconfiguredTargetNodeFactory, cell);
  }

  @Override
  public ComputationIdentifier<UnconfiguredTargetNode> getIdentifier() {
    return BuildTargetToUnconfiguredTargetNodeKey.IDENTIFIER;
  }

  @Override
  public UnconfiguredTargetNode transform(
      BuildTargetToUnconfiguredTargetNodeKey key, ComputationEnvironment env) {
    UnconfiguredBuildTarget buildTarget = key.getBuildTarget();

    BuildFileManifest manifest = env.getDep(getManifestKey(key));
    @Nullable Map<String, Object> rawAttributes = manifest.getTargets().get(buildTarget.getName());
    if (rawAttributes == null) {
      throw new NoSuchBuildTargetException(buildTarget);
    }

    /** TODO: do it directly not using {@link UnconfiguredBuildTargetView} */
    UnconfiguredBuildTargetView unconfiguredBuildTargetView =
        ImmutableUnconfiguredBuildTargetView.of(cell.getRoot(), buildTarget);

    return unconfiguredTargetNodeFactory.create(
        cell,
        cell.getRoot().resolve(unconfiguredBuildTargetView.getBasePath()).resolve(buildFileName),
        unconfiguredBuildTargetView,
        DependencyStack.root(),
        rawAttributes);
  }

  @Override
  public ImmutableSet<? extends ComputeKey<? extends ComputeResult>> discoverDeps(
      BuildTargetToUnconfiguredTargetNodeKey key, ComputationEnvironment env) {
    return ImmutableSet.of();
  }

  @Override
  public ImmutableSet<? extends ComputeKey<? extends ComputeResult>> discoverPreliminaryDeps(
      BuildTargetToUnconfiguredTargetNodeKey key) {
    // To construct raw target node, we first need to parse a build file and obtain corresponding
    // manifest, so require it as a dependency
    return ImmutableSet.of(getManifestKey(key));
  }

  private BuildPackagePathToBuildFileManifestKey getManifestKey(
      BuildTargetToUnconfiguredTargetNodeKey key) {
    UnconfiguredBuildTarget buildTarget = key.getBuildTarget();

    /** TODO: do it directly not using {@link UnconfiguredBuildTargetView} */
    UnconfiguredBuildTargetView unconfiguredBuildTargetView =
        ImmutableUnconfiguredBuildTargetView.of(cell.getRoot(), buildTarget);

    return ImmutableBuildPackagePathToBuildFileManifestKey.of(
        unconfiguredBuildTargetView.getBasePath());
  }
}
