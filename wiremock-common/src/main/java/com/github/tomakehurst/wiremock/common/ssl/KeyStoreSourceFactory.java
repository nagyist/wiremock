/*
 * Copyright (C) 2020-2025 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.common.ssl;

import com.github.tomakehurst.wiremock.common.Exceptions;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class KeyStoreSourceFactory {

  @SuppressWarnings("unchecked")
  public static KeyStoreSource getAppropriateForJreVersion(
      String path, String keyStoreType, char[] keyStorePassword) {
    try {
      final Class<? extends KeyStoreSource> theClass =
          (Class<? extends KeyStoreSource>)
              Class.forName(
                  "com.github.tomakehurst.wiremock.common.ssl.WritableFileOrClasspathKeyStoreSource");
      return safelyGetConstructor(theClass, String.class, String.class, char[].class)
          .newInstance(path, keyStoreType, keyStorePassword);
    } catch (ClassNotFoundException
        | IllegalAccessException
        | InstantiationException
        | InvocationTargetException e) {
      return new ReadOnlyFileOrClasspathKeyStoreSource(path, keyStoreType, keyStorePassword);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> Constructor<T> safelyGetConstructor(
      Class<T> clazz, Class<?>... parameterTypes) {
    try {
      return clazz.getConstructor(parameterTypes);
    } catch (NoSuchMethodException e) {
      return Exceptions.throwUnchecked(e, Constructor.class);
    }
  }
}
