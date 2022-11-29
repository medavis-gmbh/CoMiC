/*-
 * #%L
 * License Compliance Tool
 * %%
 * Copyright (C) 2022 medavis GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package de.medavis.lct.core.downloader;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.UserLogger;
import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;

@ExtendWith(MockitoExtension.class)
class LicenseFileDownloaderTest {

    private static final String VIEW_URL = "view";
    private static final String DOWNLOAD_URL = "download";
    private final static String BASE_URL = "http://my-host";
    private static final String DOWNLOAD_EXT = ".txt";

    @TempDir
    private Path outputPath;
    @TempDir
    private Path cachePath;

    @Mock
    private Configuration configuration;
    @Mock
    private ComponentLister componentLister;
    @Mock(strictness = Strictness.LENIENT)
    private FileDownloader fileDownloader;
    @Mock
    private UserLogger userLogger;

    private LicenseDownloader underTest;

    private Map<String, byte[]> downloads = new LinkedHashMap<>();

    @Test
    void shouldDownloadAllLicensesFromAllComponents() throws IOException {
        setup(
                component(
                        license("A", true, true),
                        license("B", true, true)),
                component(license("C", true, true))
        );

        invokeDownload();

        verifyLicenses("A", "B", "C");
        verifyDownloaded(DOWNLOAD_URL, "A", "B", "C");
    }

    @Test
    void shouldDownloadSameLicenseOnlyOnce() throws IOException {
        setup(
                component(license("A", true, true)),
                component(license("A", true, true))
        );

        invokeDownload();

        verifyLicenses("A");
        verifyDownloaded(DOWNLOAD_URL, "A");
    }

    @Test
    void shouldUseViewUrlIfDownloadUrlIsNotSet() throws IOException {
        setup(component(license("A", true, false)));

        invokeDownload();

        verifyLicenses("A");
        verifyDownloaded(VIEW_URL, "A");
    }

    @Test
    void shouldNotDownloadLicenseIfNoUrlIsSet() throws IOException {
        setup(component(license("A", false, false)));

        invokeDownload();

        verifyEmptyLicenses();
        verifyNothingDownloaded();
    }

    @Test
    void shouldPopulateCache() throws IOException {
        setup(component(license("A", true, true)));

        invokeDownload();

        verifyCache("A");
    }

    @Test
    void shouldCreateCachePathIfNotExists() throws IOException {
        this.cachePath = cachePath.resolve("new-subdir");
        setup(component(license("A", true, true)));

        invokeDownload();

        verifyCache("A");
    }

    @Test
    void shouldWorkWithoutCache() throws IOException {
        setup(false, component(license("A", true, true)));

        invokeDownload();

        verifyLicenses("A");
        verifyEmptyCache();
    }

    @Test
    void shouldNotDownloadLicensesIfItExistsInCache() throws IOException {
        initializeCache("A", DOWNLOAD_EXT);
        setup(component(license("A", true, true)));

        invokeDownload();

        verifyLicenses("A");
        verifyNothingDownloaded();
    }

    private void initializeCache(String licenseName, String extension) throws IOException {
        String filename = licenseName;
        if (extension != null) {
            filename = filename + extension;
        }
        Files.asCharSink(cachePath.resolve(filename).toFile(), StandardCharsets.UTF_8).write(licenseName);
    }

    private void setup(ComponentData... components) {
        setup(true, components);
    }

    private void setup(boolean cache, ComponentData... components) {
        when(configuration.getLicenseCachePathOptional()).thenReturn(cache ? Optional.of(cachePath) : Optional.empty());
        when(componentLister.listComponents(any())).thenReturn(Arrays.asList(components));
        underTest = new LicenseDownloader(componentLister, configuration, fileDownloader);
    }

    private ComponentData component(License... licenses) {
        return new ComponentData(
                RandomStringUtils.randomAlphabetic(6),
                RandomStringUtils.randomAlphabetic(6),
                RandomStringUtils.randomAlphabetic(6),
                ImmutableSet.copyOf(licenses),
                Collections.emptySet());
    }

    private License license(String name, boolean hasViewUrl, boolean hasDownloadUrl) throws IOException {
        String viewUrl = stubDownload(hasViewUrl, VIEW_URL, name);
        String downloadUrl = stubDownload(hasDownloadUrl, DOWNLOAD_URL, name);
        return new License(name, viewUrl, downloadUrl);
    }

    private String stubDownload(boolean hasUrl, String prefix, String licenseName) throws IOException {
        String url = null;
        if (hasUrl) {
            String relativeUrl = createUrl(prefix, licenseName);
            url = BASE_URL + relativeUrl;
            doAnswer(invocation -> {
                DownloadHandler handler = invocation.getArgument(2, DownloadHandler.class);
                handler.handle(licenseName + DOWNLOAD_EXT, licenseName.getBytes(StandardCharsets.UTF_8));
                return null;
            }).when(fileDownloader).downloadToFile(eq(url), any(), any());
        }
        return url;
    }

    private String createUrl(String... parts) {
        return "/" + Joiner.on("/").join(parts);
    }

    private void invokeDownload() throws IOException {
        underTest.download(userLogger, new ByteArrayInputStream(new byte[0]), downloads::put);
    }

    private void verifyLicenses(String... licenses) {
        assertSoftly(softly -> Stream.of(licenses)
                .forEach(license -> assertThat(downloads).containsEntry(license + DOWNLOAD_EXT, license.getBytes(StandardCharsets.UTF_8))));
    }

    private void verifyEmptyLicenses() {
        assertThat(outputPath).isEmptyDirectory();
    }

    private void verifyDownloaded(String prefix, String... licenses) throws IOException {
        for (String license : licenses) {
            Mockito.verify(fileDownloader).downloadToFile(eq(BASE_URL + createUrl(prefix, license)), any(), any());
        }
    }

    private void verifyNothingDownloaded() {
        Mockito.verifyNoInteractions(fileDownloader);
    }

    private void verifyCache(String... licenses) {
        assertSoftly(softly -> Stream.of(licenses).forEach(license ->
                assertThat(cachePath.resolve(license + DOWNLOAD_EXT))
                        .exists()
                        .hasContent(license)));
    }

    private void verifyEmptyCache() {
        assertThat(cachePath).isEmptyDirectory();
    }

    private enum UrlBehaviour {
        SUCCESS,
        FAILURE,
        REDIRECT;
    }
}
