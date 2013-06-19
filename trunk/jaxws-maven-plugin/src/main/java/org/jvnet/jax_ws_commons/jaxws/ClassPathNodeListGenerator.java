/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jvnet.jax_ws_commons.jaxws;

import java.util.ArrayList;
import java.util.List;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

/**
 *
 * @author lukas
 */
final class ClassPathNodeListGenerator extends PreorderNodeListGenerator {

    private final List<DependencyNode> endorsedNodes;
    private boolean endorsed;

    public ClassPathNodeListGenerator() {
        endorsedNodes = new ArrayList<DependencyNode>(8);
        endorsed = false;
    }

    public void setEndorsed(boolean endorsed) {
        this.endorsed = endorsed;
    }

    @Override
    public List<DependencyNode> getNodes() {
        List<DependencyNode> retVal = new ArrayList<DependencyNode>(nodes);
        if (endorsed) {
            retVal.retainAll(endorsedNodes);
        } else {
            retVal.removeAll(endorsedNodes);
        }
        return retVal;
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        Artifact a = node.getDependency().getArtifact();
        if ("jaxws-api".equals(a.getArtifactId()) || "jaxb-api".equals(a.getArtifactId())
                    || "saaj-api".equals(a.getArtifactId()) || "jsr181-api".equals(a.getArtifactId())
                    || "javax.annotation".equals(a.getArtifactId())
                    || "javax.annotation-api".equals(a.getArtifactId())
                    || "webservices-api".equals(a.getArtifactId())) {
            endorsedNodes.add(node);
        }
        return super.visitEnter(node);
    }
}
