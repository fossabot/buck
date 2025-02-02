/*
 * Copyright 2013-present Facebook, Inc.
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

package com.facebook.buck.rules.coercer;

import com.facebook.buck.core.cell.CellPathResolver;
import com.facebook.buck.core.model.TargetConfiguration;
import com.facebook.buck.io.filesystem.ProjectFilesystem;
import com.facebook.buck.rules.coercer.concat.Concatable;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import javax.annotation.Nullable;

/**
 * Class defining an interpretation of some dynamically typed Java object as a specific class.
 *
 * <p>Used to coerce JSON parser output from BUCK files into the proper type to populate Description
 * rule args.
 *
 * @param <T> resulting type
 */
public interface TypeCoercer<T> extends Concatable<T> {

  Class<T> getOutputClass();

  /**
   * Returns whether the leaf nodes of this type coercer outputs value that is an instance of the
   * given class or its subclasses. Does not match non-leaf nodes like Map or List.
   */
  boolean hasElementClass(Class<?>... types);

  /**
   * Traverse an object guided by this TypeCoercer.
   *
   * <p>#{link Traversal#traverse} function will be called once for the object. If the object is a
   * collection or map, it will also recursively traverse all elements of the map.
   */
  void traverse(CellPathResolver cellRoots, T object, Traversal traversal);

  /** @throws CoerceFailedException Input object cannot be coerced into the given type. */
  T coerce(
      CellPathResolver cellRoots,
      ProjectFilesystem filesystem,
      Path pathRelativeToProjectRoot,
      TargetConfiguration targetConfiguration,
      Object object)
      throws CoerceFailedException;

  /**
   * Implementation of concatenation for this type. <code>null</code> indicates that concatenation
   * isn't supported by the type.
   */
  @Nullable
  @Override
  default T concat(Iterable<T> elements) {
    return null;
  }

  /** @return {@code true} is this coercer supports concatenation. */
  default boolean supportsConcatenation() {
    return concat(ImmutableList.of()) != null;
  }

  interface Traversal {
    void traverse(Object object);
  }
}
