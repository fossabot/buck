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

package com.facebook.buck.shell;

import com.facebook.buck.android.toolchain.AndroidTools;
import com.facebook.buck.core.model.BuildTarget;
import com.facebook.buck.core.rules.BuildRuleParams;
import com.facebook.buck.core.rules.BuildRuleResolver;
import com.facebook.buck.core.rules.tool.BinaryBuildRule;
import com.facebook.buck.core.toolchain.tool.Tool;
import com.facebook.buck.core.toolchain.tool.impl.CommandTool;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.rules.args.Arg;
import com.facebook.buck.rules.args.SourcePathArg;
import com.facebook.buck.rules.coercer.SourceSet;
import com.facebook.buck.sandbox.SandboxExecutionStrategy;
import java.util.Optional;

/** Same as a Genrule, but marked as a binary. */
public class GenruleBinary extends Genrule implements BinaryBuildRule {
  protected GenruleBinary(
      BuildTarget buildTarget,
      ProjectFilesystem projectFilesystem,
      SandboxExecutionStrategy sandboxExecutionStrategy,
      BuildRuleResolver resolver,
      BuildRuleParams params,
      SourceSet srcs,
      Optional<Arg> cmd,
      Optional<Arg> bash,
      Optional<Arg> cmdExe,
      Optional<String> type,
      String out,
      boolean isCacheable,
      Optional<String> environmentExpansionSeparator,
      Optional<AndroidTools> androidTools) {
    super(
        buildTarget,
        projectFilesystem,
        resolver,
        params,
        sandboxExecutionStrategy,
        srcs,
        cmd,
        bash,
        cmdExe,
        type,
        out,
        false,
        isCacheable,
        environmentExpansionSeparator,
        androidTools);
  }

  @Override
  public Tool getExecutableCommand() {
    return new CommandTool.Builder().addArg(SourcePathArg.of(getSourcePathToOutput())).build();
  }
}
