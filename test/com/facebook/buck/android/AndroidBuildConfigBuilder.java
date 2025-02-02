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
package com.facebook.buck.android;

import static com.facebook.buck.jvm.java.JavaCompilationConstants.ANDROID_JAVAC_OPTIONS;

import com.facebook.buck.android.toolchain.AndroidPlatformTarget;
import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.model.targetgraph.AbstractNodeBuilder;
import com.facebook.buck.core.toolchain.impl.ToolchainProviderBuilder;
import com.facebook.buck.jvm.java.JavaCompilationConstants;
import com.facebook.buck.jvm.java.toolchain.JavaToolchain;
import com.facebook.buck.jvm.java.toolchain.JavacOptionsProvider;

public class AndroidBuildConfigBuilder
    extends AbstractNodeBuilder<
        AndroidBuildConfigDescriptionArg.Builder,
        AndroidBuildConfigDescriptionArg,
        AndroidBuildConfigDescription,
        AndroidBuildConfig> {

  public AndroidBuildConfigBuilder(BuildTarget target) {
    super(
        new AndroidBuildConfigDescription(
            new ToolchainProviderBuilder()
                .withToolchain(
                    JavacOptionsProvider.DEFAULT_NAME,
                    JavacOptionsProvider.of(ANDROID_JAVAC_OPTIONS))
                .withToolchain(
                    AndroidPlatformTarget.DEFAULT_NAME, TestAndroidPlatformTargetFactory.create())
                .withToolchain(
                    JavaToolchain.DEFAULT_NAME, JavaCompilationConstants.DEFAULT_JAVA_TOOLCHAIN)
                .build()),
        target);
  }

  public AndroidBuildConfigBuilder setPackage(String packageName) {
    getArgForPopulating().setPackage(packageName);
    return this;
  }
}
