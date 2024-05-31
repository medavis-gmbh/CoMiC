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
package de.medavis.lct.core;

import java.net.URL;
import java.util.Optional;
import java.util.Set;

public interface Configuration {

    default Optional<URL> getComponentMetadataUrl() {
        return Optional.empty();
    }

    default Optional<URL> getLicensesUrl() {
        return Optional.empty();
    }

    default Optional<URL> getLicenseMappingsUrl() {
        return Optional.empty();
    }

    /**
     * Used by license patcher feature.
     *
     * @return Optional URL
     */
    default Optional<URL> getLicensePatchingRulesUrl() {
        return Optional.empty();
    }

    /**
     * Used by license patcher feature.
     *
     * @return Optional URL
     */
    default Optional<URL> getSpdxLicensesUrl() {
        return Optional.empty();
    }

    /**
     * Used by license patcher feature.
     *
     * @return Optional set of to be skipped group names;
     */
    default Optional<Set<String>> getSkipGroupNameSet() {
        return Optional.empty();
    }

    /**
     * Used by license patcher feature.
     *
     * @return Boolean
     */
    default boolean isResolveExpressions() {
        return false;
    }

}
