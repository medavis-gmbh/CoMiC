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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

class FilesystemCache implements Cache {

    private final Path path;

    public FilesystemCache(Path path) {
        this.path = path;
    }

    @Override
    public Optional<File> getCachedFile(String licenseName) {
        return Optional.of(path.resolve(licenseName).toFile()).filter(File::isFile);
    }

    @Override
    public void addCachedFile(String licenseName, File source) throws IOException {
        final Path target = path.resolve(licenseName);
        Files.createDirectories(target);
        Files.copy(source.toPath(), target, REPLACE_EXISTING);
    }
}
