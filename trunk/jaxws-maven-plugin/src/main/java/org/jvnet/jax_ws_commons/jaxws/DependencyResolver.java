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
import java.util.Collection;
import java.util.List;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.Exclusion;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 *
 * @author lukas
 */
final class DependencyResolver {

    public static DependencyResult resolve(CollectRequest collectRequest, List<RemoteRepository> remoteRepos,
            RepositorySystem repoSystem, RepositorySystemSession repoSession) throws DependencyResolutionException {
        DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
        return repoSystem.resolveDependencies(repoSession, dependencyRequest);
    }

    public static DependencyResult resolve(org.apache.maven.artifact.Artifact artifact, List<RemoteRepository> remoteRepos,
            RepositorySystem repoSystem, RepositorySystemSession repoSession) throws DependencyResolutionException {
        Artifact a = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), "jar", artifact.getVersion());
        Dependency dependency = new Dependency(a, null);
        CollectRequest collectRequest = new CollectRequest(dependency, remoteRepos);
        return resolve(collectRequest, remoteRepos, repoSystem, repoSession);
    }

    public static DependencyResult resolve(org.apache.maven.model.Dependency dependency, List<RemoteRepository> remoteRepos,
            RepositorySystem repoSystem, RepositorySystemSession repoSession) throws DependencyResolutionException {
        CollectRequest collectRequest = new CollectRequest(createDependency(dependency), remoteRepos);
        return resolve(collectRequest, remoteRepos, repoSystem, repoSession);
    }

    private static Dependency createDependency(org.apache.maven.model.Dependency d) {
        Collection<Exclusion> toExclude = new ArrayList<Exclusion>();
        for (org.apache.maven.model.Exclusion e : d.getExclusions()) {
            toExclude.add(new Exclusion(e.getGroupId(), e.getArtifactId(), null, "jar"));
        }
        Artifact artifact = new DefaultArtifact(d.getGroupId(), d.getArtifactId(), "jar", d.getVersion());
        return new Dependency(artifact, d.getScope(), d.isOptional(), toExclude);
    }

}
