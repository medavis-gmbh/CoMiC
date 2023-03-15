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
package de.medavis.lct.core.asset;

import com.google.common.collect.ImmutableSet;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import de.medavis.lct.core.license.License;

class AssetLoaderTest {

    private final static String SAMPLE_BOM = "/asset/test-bom.json";

    private final AssetLoader underTest = new AssetLoader();

    @Test
    void shouldLoadAssetFromBOM() {
        InputStream sampleBomStream = getClass().getResourceAsStream(SAMPLE_BOM);

        Asset actual = underTest.loadFromBom(sampleBomStream);

        assertThat(actual.name()).isEqualTo("de.medavis.bommanager");
        assertThat(actual.version()).isEqualTo("1.0-SNAPSHOT");
        assertThat(actual.components()).containsExactlyInAnyOrder(
                new Component("ch.qos.logback", "logback-classic", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                        License.dynamic("EPL-1.0", null),
                        License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                )),
                new Component("ch.qos.logback", "logback-core", "1.2.11", "https://github.com/ceki/logback", ImmutableSet.of(
                        License.dynamic("EPL-1.0", null),
                        License.dynamic("GNU Lesser General Public License", "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
                )),
                new Component("org.slf4j", "slf4j-api", "1.7.32", "https://github.com/qos-ch/slf4j", ImmutableSet.of(
                        License.dynamic("MIT", "https://opensource.org/licenses/MIT")))
        );
    }
}
