/*
 * Copyright (C) 2016-2025 Thomas Akehurst
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
package com.github.tomakehurst.wiremock.standalone;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.testsupport.TestFiles.filePath;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.tomakehurst.wiremock.common.ClasspathFileSource;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.common.NotWritableException;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.common.filemaker.FilenameMaker;
import com.github.tomakehurst.wiremock.stubbing.InMemoryStubMappings;
import com.github.tomakehurst.wiremock.stubbing.StoreBackedStubMappings;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonFileMappingsSourceTest {

  @TempDir public File tempDir;

  StoreBackedStubMappings stubMappings;
  JsonFileMappingsSource source;
  File stubMappingFile;

  @BeforeEach
  public void init() throws Exception {
    stubMappings = new InMemoryStubMappings();
  }

  private void configureWithMultipleMappingFile() throws Exception {
    stubMappingFile = File.createTempFile("multi", ".json", tempDir);
    Files.copy(
        Paths.get(filePath("multi-stub/multi.json")),
        stubMappingFile.toPath(),
        StandardCopyOption.REPLACE_EXISTING);
    load();
  }

  private void configureWithSingleMappingFile() throws Exception {
    stubMappingFile = File.createTempFile("single", ".json", tempDir);
    Files.copy(
        Paths.get(filePath("multi-stub/single.json")),
        stubMappingFile.toPath(),
        StandardCopyOption.REPLACE_EXISTING);
    load();
  }

  private void load() {
    source = new JsonFileMappingsSource(new SingleRootFileSource(tempDir), new FilenameMaker());
    source.loadMappingsInto(stubMappings);
  }

  @Test
  void loadsMappingsViaClasspathFileSource() {
    ClasspathFileSource fileSource = new ClasspathFileSource("jar-filesource");
    JsonFileMappingsSource source = new JsonFileMappingsSource(fileSource, new FilenameMaker());
    StoreBackedStubMappings stubMappings = new InMemoryStubMappings();

    source.loadMappingsInto(stubMappings);

    List<StubMapping> allMappings = stubMappings.getAll();
    assertThat(allMappings, hasSize(2));

    List<String> mappingRequestUrls =
        asList(allMappings.get(0).getRequest().getUrl(), allMappings.get(1).getRequest().getUrl());
    assertThat(mappingRequestUrls, is(asList("/second_test", "/test")));
  }

  @Test
  void stubMappingFilesAreWrittenWithInsertionIndex() throws Exception {
    JsonFileMappingsSource source =
        new JsonFileMappingsSource(new SingleRootFileSource(tempDir), new FilenameMaker());

    StubMapping stub = get("/saveable").willReturn(ok()).build();
    source.save(stub);

    File savedFile = Objects.requireNonNull(tempDir.listFiles())[0];
    String savedStub = Files.readString(savedFile.toPath());

    assertThat(savedStub, containsString("\"insertionIndex\" : 0"));
  }

  @Test
  void stubMappingFilesWithOwnFileTemplateFormat() {
    JsonFileMappingsSource source =
        new JsonFileMappingsSource(
            new SingleRootFileSource(tempDir),
            new FilenameMaker("{{{request.method}}}-{{{request.url}}}.json"));

    StubMapping stub = get("/saveable").willReturn(ok()).build();
    source.save(stub);

    File savedFile = Objects.requireNonNull(tempDir.listFiles())[0];

    assertEquals("get-saveable.json", savedFile.getName());
  }

  @Test
  void refusesToRemoveStubMappingContainedInMultiFile() throws Exception {
    configureWithMultipleMappingFile();

    StubMapping firstStub = stubMappings.getAll().get(0);

    try {
      source.remove(firstStub.getId());
      fail("Expected an exception to be thrown");
    } catch (Exception e) {
      assertThat(e, Matchers.instanceOf(NotWritableException.class));
      assertThat(
          e.getMessage(),
          is(
              "Stubs loaded from multi-mapping files are read-only, and therefore cannot be removed"));
    }

    assertThat(stubMappingFile.exists(), is(true));
  }

  @Test
  void refusesToRemoveAllWhenMultiMappingFilesArePresent() throws Exception {
    configureWithMultipleMappingFile();

    try {
      source.removeAll();
      fail("Expected an exception to be thrown");
    } catch (Exception e) {
      assertThat(e, Matchers.instanceOf(NotWritableException.class));
      assertThat(
          e.getMessage(),
          is(
              "Some stubs were loaded from multi-mapping files which are read-only, so remove all cannot be performed"));
    }

    assertThat(stubMappingFile.exists(), is(true));
  }

  @Test
  void refusesToSaveStubMappingOriginallyLoadedFromMultiMappingFile() throws Exception {
    configureWithMultipleMappingFile();

    StubMapping firstStub = stubMappings.getAll().get(0);

    try {
      source.save(firstStub);
      fail("Expected an exception to be thrown");
    } catch (Exception e) {
      assertThat(e, Matchers.instanceOf(NotWritableException.class));
      assertThat(
          e.getMessage(),
          is("Stubs loaded from multi-mapping files are read-only, and therefore cannot be saved"));
    }

    assertThat(stubMappingFile.exists(), is(true));
  }

  @Test
  void savesStubMappingOriginallyLoadedFromSingleMappingFile() throws Exception {
    configureWithSingleMappingFile();

    StubMapping firstStub = stubMappings.getAll().get(0);
    firstStub.setName("New name");
    source.save(firstStub);

    assertThat(Files.readString(stubMappingFile.toPath()), containsString("New name"));
  }

  @Test
  void removesStubMappingOriginallyLoadedFromSingleMappingFile() throws Exception {
    configureWithSingleMappingFile();

    StubMapping firstStub = stubMappings.getAll().get(0);
    source.remove(firstStub.getId());

    assertThat(stubMappingFile.exists(), is(false));
  }

  @Test
  void canSetAllStubFilesInSource() throws IOException {
    StubMapping existingStub1 = get("/thing/1").withName("thing1").willReturn(ok()).build();
    File existingStub1File = new File(tempDir, "thing1-" + existingStub1.getId() + ".json");
    Files.writeString(existingStub1File.toPath(), Json.writePrivate(existingStub1));

    StubMapping existingStub2 = get("/thing/1").withName("thing1").willReturn(noContent()).build();
    File existingStub2File = new File(tempDir, "thing1-" + existingStub2.getId() + ".json");
    Files.writeString(existingStub2File.toPath(), Json.writePrivate(existingStub2));

    File existingStub3File = File.createTempFile("single3", ".json", tempDir);
    Files.writeString(existingStub3File.toPath(), "{}");

    assertThat(
        Arrays.stream(Objects.requireNonNull(tempDir.listFiles())).toList(),
        containsInAnyOrder(existingStub1File, existingStub2File, existingStub3File));

    load();

    StubMapping newStub1 =
        post("/create")
            .withName("thing1")
            .withId(existingStub2.getId())
            .willReturn(created())
            .build();
    StubMapping newStub2 = get("/thing/2").withName("thing2").willReturn(created()).build();
    source.setAll(List.of(newStub1, newStub2));

    File newStub2File = new File(tempDir, "thing2-" + newStub2.getId() + ".json");
    assertThat(
        Arrays.stream(Objects.requireNonNull(tempDir.listFiles())).toList(),
        containsInAnyOrder(existingStub2File, newStub2File));
    assertThat(Files.readString(existingStub2File.toPath()), is(Json.writePrivate(newStub1)));
    assertThat(Files.readString(newStub2File.toPath()), is(Json.writePrivate(newStub2)));
  }
}
