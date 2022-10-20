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

import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.compress.utils.FileNameUtils;

import static com.google.common.base.MoreObjects.firstNonNull;

import de.medavis.lct.core.Configuration;
import de.medavis.lct.core.UserLogger;
import de.medavis.lct.core.license.License;
import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;

public class LicenseDownloader {

    private final ComponentLister componentLister;
    private final Cache cache;
    private final FileDownloader fileDownloader;

    public LicenseDownloader(ComponentLister componentLister, Configuration configuration, FileDownloader fileDownloader) {
        this.componentLister = componentLister;
        this.fileDownloader = fileDownloader;
        this.cache = configuration.getLicenseCachePathOptional()
                .map(FilesystemCache::new)
                .map(Cache.class::cast)
                .orElseGet(CacheDisabled::new);
    }

    public void download(UserLogger userLogger, InputStream inputStream, TargetHandler targetHandler) throws IOException {
        final List<ComponentData> components = componentLister.listComponents(inputStream);
        Set<License> licenses = components.stream()
                .map(ComponentData::getLicenses)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, File> cachedLicenses = new LinkedHashMap<>();
        for (License license : licenses) {
            Optional<File> cachedLicenseFile = cache.getCachedFile(license.getName());
            cachedLicenseFile.ifPresent(file -> cachedLicenses.put(license.getName(), file));
        }
        userLogger.info("Using %d licenses from cache.%n", cachedLicenses.size());
        cachedLicenses.forEach((name, file) -> copyFromCache(name, file, userLogger, targetHandler));

        Map<String, String> downloadUrls = licenses.stream()
                .filter(license -> !cachedLicenses.containsKey(license.getName()))
                .filter(license -> !Strings.isNullOrEmpty(license.getDownloadUrl()) || !Strings.isNullOrEmpty(license.getUrl()))
                .collect(Collectors.toMap(License::getName, license -> firstNonNull(license.getDownloadUrl(), license.getUrl())));
        userLogger.info("Will download %d licenses.%n", downloadUrls.size());

        int index = 1;
        for (Entry<String, String> entry : downloadUrls.entrySet()) {
            String name = entry.getKey();
            String url = entry.getValue();
            downloadFile(name, url, userLogger, cacheDecorator(targetHandler), index, downloadUrls.size());
            index++;
        }
    }

    private void copyFromCache(String licenseName, File cachedFile, UserLogger userLogger, TargetHandler targetHandler) {
        String extension = FileNameUtils.getExtension(cachedFile.getName());
        if (!Strings.isNullOrEmpty(extension)) {
            // TODO Rework extension handling so that "." is not part of the extension itself
            extension = "." + extension;
        }
        try {
            targetHandler.handle(licenseName, extension, Files.readAllBytes(cachedFile.toPath()));
        } catch (IOException e) {
            userLogger.error("Could not copy license file from cache: %s.%n", e.getMessage());
        }
    }

    private void downloadFile(String licenseName, String source, UserLogger userLogger, TargetHandler targetHandler, int index, int size) {
        try {
            userLogger.info("(%d/%d) Downloading license %s from %s... ", index, size, licenseName, source);
            fileDownloader.downloadToFile(source, licenseName, targetHandler);
            userLogger.info("Done.%n");
        } catch (IOException e) {
            userLogger.error("Could not download license file: %s.%n", e.getMessage());
        }
    }

    private TargetHandler cacheDecorator(TargetHandler targetHandler) {
        return (name, extension, content) -> {
            cache.addCachedFile(name, extension, content);
            targetHandler.handle(name, extension, content);
        };
    }

}
