/*
 * Copyright (C) 2022-2025 Thomas Akehurst
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
package com.github.tomakehurst.wiremock.admin.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScenarioState {

  private final String state;

  public ScenarioState(@JsonProperty("state") String state) {
    this.state = state;
  }

  public String getState() {
    return state;
  }
}
