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
package de.medavis.lct.jenkins.create;

import com.google.common.io.Resources;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Run.Artifact;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import jenkins.util.VirtualFile;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import de.medavis.lct.core.list.ComponentData;
import de.medavis.lct.core.list.ComponentLister;
import de.medavis.lct.core.outputter.FreemarkerOutputter;
import de.medavis.lct.util.InputStreamContentArgumentMatcher;

@ExtendWith(MockitoExtension.class)
@WithJenkins
class CreateManifestBuilderTest {

    private static final String INPUT_PATH = "input.bom";
    private static final String OUTPUT_FILE_EXTENSION = ".html";
    private static final String OUTPUT_PATH = "output" + OUTPUT_FILE_EXTENSION;
    private static final String ARCHIVE_FILENAME = CreateManifestBuilder.ARCHIVE_FILE_NAME + OUTPUT_FILE_EXTENSION;
    private static final String TEMPLATE_URL = "file://template.ftl";
    private static final List<ComponentData> COMPONENT_LIST = Collections.singletonList(
            new ComponentData("name", "version", "url", Collections.emptySet(), Collections.emptySet()));
    private static final String FAKE_SBOM = "Normally, this would be a CycloneDX SBOM.";
    private static final String FAKE_MANIFEST = "IRL, I would be the manifest";

    @Mock(strictness = Strictness.LENIENT)
    private ComponentLister componentListerMock;
    @Mock(strictness = Strictness.LENIENT)
    private FreemarkerOutputter outputterMock;

    @BeforeEach
    public void setUp() throws IOException {
        CreateManifestBuilderFactory.setComponentLister(componentListerMock);
        when(componentListerMock.listComponents(argThat(new InputStreamContentArgumentMatcher(FAKE_SBOM)))).thenReturn(COMPONENT_LIST);

        CreateManifestBuilderFactory.setOutputter(outputterMock);
        doAnswer(invocation -> {
            Writer writer = invocation.getArgument(1, Writer.class);
            writer.write(FAKE_MANIFEST);
            return null;
        }).when(outputterMock).output(any(), any(), any());
    }

    @Test
    void testConfigRoundtripDefaultFormat(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        final CreateManifestBuilder builder = new CreateManifestBuilder(INPUT_PATH, OUTPUT_PATH);
        builder.setTemplateUrl(TEMPLATE_URL);
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        jenkins.assertEqualDataBoundBeans(builder, project.getBuildersList().get(0));
    }

    @Test
    void testScriptedPipelineBuild(JenkinsRule jenkins) throws Exception {
        String agentLabel = "any";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource("scriptedPipeline.groovy"), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        assertThat(run.getArtifacts()).extracting(Artifact::getFileName).containsExactly(ARCHIVE_FILENAME);
        final VirtualFile outputFile = run.getArtifactManager().root().child(ARCHIVE_FILENAME);
        try {
            assertThat(outputFile.canRead()).isTrue();
            assertThat(outputFile.open()).hasContent(FAKE_MANIFEST);
        } catch (IOException e) {
            fail("Unexpected exception", e);
        }
    }

    @Test
    void testDeclarativePipelineBuild(JenkinsRule jenkins) throws Exception {
        String agentLabel = "any";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-declarative-pipeline");
        String pipelineScript = Resources.toString(getClass().getResource("declarativePipeline.groovy"), Charset.defaultCharset());
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        assertThat(run.getArtifacts()).extracting(Artifact::getFileName).containsExactly(ARCHIVE_FILENAME);
        final VirtualFile outputFile = run.getArtifactManager().root().child(ARCHIVE_FILENAME);
        try {
            assertThat(outputFile.canRead()).isTrue();
            assertThat(outputFile.open()).hasContent(FAKE_MANIFEST);
        } catch (IOException e) {
            fail("Unexpected exception", e);
        }
    }

}
