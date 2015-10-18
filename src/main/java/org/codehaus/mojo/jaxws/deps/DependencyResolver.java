/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.codehaus.mojo.jaxws.deps;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.NotDependencyFilter;
import org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.artifact.DefaultArtifact;

/**
 *
 * @author lukas
 */
public final class DependencyResolver {
    private final List<RemoteRepository> remoteRepos;
    private final RepositorySystem repoSystem;
    private final RepositorySystemSession repoSession;

    private final Set<org.eclipse.aether.artifact.Artifact> endorsedCp = new HashSet<org.eclipse.aether.artifact.Artifact>();
    private final Map<String, org.eclipse.aether.artifact.Artifact> cp = new HashMap<String, org.eclipse.aether.artifact.Artifact>();

    public DependencyResolver(List<RemoteRepository> remoteRepos, RepositorySystem repoSystem,
                              RepositorySystemSession repoSession) {
        this.remoteRepos = remoteRepos;
        this.repoSystem = repoSystem;
        this.repoSession = repoSession;
    }

    public void resolve(CollectRequest collectRequest, DependencyFilter filter)
                    throws DependencyResolutionException {
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);
        sortArtifacts(repoSystem.resolveDependencies(repoSession, dependencyRequest));
    }

    public void resolve(org.apache.maven.artifact.Artifact artifact, String[] extraArtifactIDs)
            throws DependencyResolutionException {
        Artifact a = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(), artifact.getVersion());
        Dependency dependency = new Dependency(a, null);
        CollectRequest collectRequest = new CollectRequest(dependency, remoteRepos);
        resolve(collectRequest, extraArtifactIDs == null ? null : new DepFilter(extraArtifactIDs));
    }

    public void resolve(org.apache.maven.model.Dependency dependency)
            throws DependencyResolutionException {
        CollectRequest collectRequest = new CollectRequest(createDependency(dependency), remoteRepos);
        resolve(collectRequest, new ExclusionFilter(dependency.getExclusions()));
    }

    public Set<String> getCp() {
        return cp.keySet();
    }

    public StringBuilder getSb() {
        return getCPasString(cp.values());
    }

    public StringBuilder getEsb() {
        return getCPasString(endorsedCp);
    }

    private Dependency createDependency(org.apache.maven.model.Dependency d) {
        Collection<Exclusion> toExclude = new ArrayList<Exclusion>();
        for (org.apache.maven.model.Exclusion e : d.getExclusions()) {
            toExclude.add(new Exclusion(e.getGroupId(), e.getArtifactId(), null, "jar"));
        }
        Artifact artifact = new DefaultArtifact(d.getGroupId(), d.getArtifactId(), "jar", d.getVersion());
        return new Dependency(artifact, d.getScope(), d.isOptional(), toExclude);
    }

    private void sortArtifacts(DependencyResult result) {
        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        FilteringDependencyVisitor visitor = new FilteringDependencyVisitor(
                nlg, new NotDependencyFilter(new EndorsedFilter()));
        result.getRoot().accept(visitor);
        for (org.eclipse.aether.artifact.Artifact a : nlg.getArtifacts(false)) {
            cp.put(a.getGroupId() + ":" + a.getArtifactId(), a);
        }

        nlg = new PreorderNodeListGenerator();
        visitor = new FilteringDependencyVisitor(
                nlg, new EndorsedFilter());
        result.getRoot().accept(visitor);
        endorsedCp.addAll(nlg.getArtifacts(false));
    }

    private StringBuilder getCPasString(Collection<org.eclipse.aether.artifact.Artifact> artifacts) {
        StringBuilder sb = new StringBuilder();
        for (org.eclipse.aether.artifact.Artifact a : artifacts) {
            sb.append(a.getFile().getAbsolutePath());
            sb.append(File.pathSeparator);
        }
        return sb;
    }
    private static class DepFilter implements DependencyFilter {

        private final Set<Dep> toExclude = new HashSet<Dep>();

        public DepFilter(String[] artifacts) {
            if (artifacts != null) {
                for (String a : artifacts) {
                    int i = a.indexOf(':');
                    toExclude.add(new Dep(a.substring(0, i), a.substring(i + 1)));
                }
            }
        }

        @Override
        public boolean accept(DependencyNode node, List<DependencyNode> parents) {
            org.eclipse.aether.artifact.Artifact a = node.getDependency().getArtifact();
            return !toExclude.contains(new Dep(a.getGroupId(), a.getArtifactId()));
        }

        private static class Dep {

            private final String groupId;
            private final String artifactId;

            public Dep(String groupId, String artifactId) {
                this.groupId = groupId;
                this.artifactId = artifactId;
            }

            @Override
            public int hashCode() {
                int hash = 5;
                hash = 37 * hash + (this.groupId != null ? this.groupId.hashCode() : 0);
                return hash;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                final Dep other = (Dep) obj;
                if ((this.groupId == null)
                        ? (other.groupId != null)
                        : !this.groupId.equals(other.groupId)) {
                    return false;
                }
                //startsWith here is intentional
                return !((this.artifactId == null)
                        ? (other.artifactId != null)
                        : !this.artifactId.startsWith(other.artifactId));
            }
        }
    }
}
