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
package com.facebook.buck.features.apple.projectV2;

import com.facebook.buck.apple.AppleBinaryDescription;
import com.facebook.buck.apple.AppleBundleDescription;
import com.facebook.buck.apple.AppleBundleDescriptionArg;
import com.facebook.buck.apple.AppleBundleExtension;
import com.facebook.buck.apple.AppleLibraryDescription;
import com.facebook.buck.apple.AppleLibraryDescriptionArg;
import com.facebook.buck.core.model.targetgraph.TargetGraph;
import com.facebook.buck.core.model.targetgraph.TargetNode;
import com.facebook.buck.core.model.targetgraph.impl.TargetNodes;
import com.facebook.buck.core.rules.DescriptionWithTargetGraph;
import com.facebook.buck.cxx.CxxLibraryDescription;
import com.facebook.buck.util.types.Either;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;

/** Helper class to derive information about {@link TargetNode}s. */
public class NodeHelper {

  static final ImmutableSet<Class<? extends DescriptionWithTargetGraph<?>>>
      APPLE_NATIVE_DESCRIPTION_CLASSES =
          ImmutableSet.of(
              AppleBinaryDescription.class,
              AppleLibraryDescription.class,
              CxxLibraryDescription.class);

  static final ImmutableSet<AppleBundleExtension> APPLE_NATIVE_BUNDLE_EXTENSIONS =
      ImmutableSet.of(AppleBundleExtension.APP, AppleBundleExtension.FRAMEWORK);

  static final ImmutableSet<Class<? extends DescriptionWithTargetGraph<?>>>
      APPLE_NATIVE_LIBRARY_DESCRIPTION_CLASSES =
          ImmutableSet.of(AppleLibraryDescription.class, CxxLibraryDescription.class);

  static final ImmutableSet<AppleBundleExtension> APPLE_NATIVE_LIBRARY_BUNDLE_EXTENSIONS =
      ImmutableSet.of(AppleBundleExtension.FRAMEWORK);

  static boolean isModularAppleLibrary(TargetNode<?> libraryNode) {
    Optional<TargetNode<AppleLibraryDescriptionArg>> appleLibNode =
        TargetNodes.castArg(libraryNode, AppleLibraryDescriptionArg.class);
    if (appleLibNode.isPresent()) {
      AppleLibraryDescriptionArg constructorArg = appleLibNode.get().getConstructorArg();
      return constructorArg.isModular();
    }

    return false;
  }

  /**
   * @return The Apple description compatible target node, which may be the @{code targetNode} or a
   *     node set as the binary if {@code targetNode} is a bundle description type.
   */
  static Optional<TargetNode<CxxLibraryDescription.CommonArg>> getAppleNativeNode(
      TargetGraph targetGraph, TargetNode<?> targetNode) {
    return getAppleNativeNodeOfType(
        targetGraph, targetNode, APPLE_NATIVE_DESCRIPTION_CLASSES, APPLE_NATIVE_BUNDLE_EXTENSIONS);
  }

  /**
   * @return The Apple library description compatible target node, which may be the @{code
   *     targetNode} or a node set as the binary if {@code targetNode} is a bundle description type.
   */
  static Optional<TargetNode<CxxLibraryDescription.CommonArg>> getLibraryNode(
      TargetGraph targetGraph, TargetNode<?> targetNode) {
    return getAppleNativeNodeOfType(
        targetGraph,
        targetNode,
        APPLE_NATIVE_LIBRARY_DESCRIPTION_CLASSES,
        APPLE_NATIVE_LIBRARY_BUNDLE_EXTENSIONS);
  }

  /**
   * @return The {@code targetNode} if it is of an description type contained within {@nodeTypes} or
   *     the node set as the binary if {@code targetNode} is a valid bundle contained in {@code
   *     bundleExtensions}.
   */
  private static Optional<TargetNode<CxxLibraryDescription.CommonArg>> getAppleNativeNodeOfType(
      TargetGraph targetGraph,
      TargetNode<?> targetNode,
      Set<Class<? extends DescriptionWithTargetGraph<?>>> nodeTypes,
      Set<AppleBundleExtension> bundleExtensions) {
    if (nodeTypes.contains(targetNode.getDescription().getClass())) {
      return TargetNodes.castArg(targetNode, CxxLibraryDescription.CommonArg.class);
    } else if (targetNode.getDescription() instanceof AppleBundleDescription) {
      TargetNode<AppleBundleDescriptionArg> bundle =
          TargetNodes.castArg(targetNode, AppleBundleDescriptionArg.class).get();
      Either<AppleBundleExtension, String> extension = bundle.getConstructorArg().getExtension();
      if (extension.isLeft() && bundleExtensions.contains(extension.getLeft())) {
        return TargetNodes.castArg(
            targetGraph.get(XcodeNativeTargetGenerator.getBundleBinaryTarget(bundle)),
            CxxLibraryDescription.CommonArg.class);
      }
    }
    return Optional.empty();
  }
}
