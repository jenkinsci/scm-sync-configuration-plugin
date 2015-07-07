package hudson.plugins.scm_sync_configuration.scms.customproviders.git.gitexe;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.log.ScmLogger;
import org.codehaus.plexus.util.cli.StreamConsumer;

import com.google.common.base.Strings;

/**
 * Copied from org.apache.maven.scm.provider.git.gitexe.command.status.GitStatusConsumer (maven-scm 1.9.1)
 * and fixed to account for https://issues.apache.org/jira/browse/SCM-772 .
 */
public class FixedGitStatusConsumer
implements StreamConsumer
{

    /**
     * The pattern used to match added file lines
     */
    private static final Pattern ADDED_PATTERN = Pattern.compile( "^A[ M]* (.*)$" );

    /**
     * The pattern used to match modified file lines
     */
    private static final Pattern MODIFIED_PATTERN = Pattern.compile( "^ *M[ M]* (.*)$" );

    /**
     * The pattern used to match deleted file lines
     */
    private static final Pattern DELETED_PATTERN = Pattern.compile( "^ *D * (.*)$" );

    /**
     * The pattern used to match renamed file lines
     */
    private static final Pattern RENAMED_PATTERN = Pattern.compile( "^R  (.*) -> (.*)$" );

    private final ScmLogger logger;

    private final File workingDirectory;

    /**
     * Entries are relative to working directory, not to the repositoryroot
     */
    private final List<ScmFile> changedFiles = new ArrayList<ScmFile>();

    private String relativeRepositoryPath;

    private final File repositoryRoot;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public FixedGitStatusConsumer (ScmLogger logger, File workingDirectory, File repositoryRoot) {
        this.logger = logger;
        this.workingDirectory = workingDirectory;
        if (repositoryRoot != null) {
            String absoluteRepositoryRoot = repositoryRoot.getAbsolutePath(); // Make sure all separators are File.separator
            // The revparse runs with fileset.getBasedir(). That of course must be under the repo root.
            String basePath = workingDirectory.getAbsolutePath();
            if (!absoluteRepositoryRoot.endsWith(File.separator)) {
                absoluteRepositoryRoot += File.separator;
            }
            if (basePath.startsWith(absoluteRepositoryRoot)) {
                String pathInsideRepo = basePath.substring(absoluteRepositoryRoot.length());
                if (!Strings.isNullOrEmpty(pathInsideRepo)) {
                    relativeRepositoryPath = pathInsideRepo;
                }
            }
        }
        this.repositoryRoot = repositoryRoot;
        // Either the workingDirectory == repositoryRoot: we have no relativeRepositoryPath set
        // Or the working  directory was a subdirectory (in the workspace!) of repositoryRoot, then
        // relativeRepositoryPath contains now the relative path to the working directory.
        //
        // It would appear that git status --porcelain always returns paths relative to the repository
        // root.
    }

    // ----------------------------------------------------------------------
    // StreamConsumer Implementation
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void consumeLine( String line )
    {
        if ( logger.isDebugEnabled() )
        {
            logger.debug( line );
        }
        if ( StringUtils.isEmpty( line ) )
        {
            return;
        }

        ScmFileStatus status = null;

        List<String> files = new ArrayList<String>();

        Matcher matcher;
        if ( ( matcher = ADDED_PATTERN.matcher( line ) ).find() )
        {
            status = ScmFileStatus.ADDED;
            files.add(ScmSyncGitUtils.dequote(matcher.group(1)));
        }
        else if ( ( matcher = MODIFIED_PATTERN.matcher( line ) ).find() )
        {
            status = ScmFileStatus.MODIFIED;
            files.add(ScmSyncGitUtils.dequote(matcher.group(1)));
        }
        else if ( ( matcher = DELETED_PATTERN.matcher( line ) ) .find() )
        {
            status = ScmFileStatus.DELETED;
            files.add(ScmSyncGitUtils.dequote(matcher.group(1)));
        }
        else if ( ( matcher = RENAMED_PATTERN.matcher( line ) ).find() )
        {
            status = ScmFileStatus.RENAMED;
            files.add(ScmSyncGitUtils.dequote(matcher.group(1)));
            files.add(ScmSyncGitUtils.dequote(matcher.group(2)));
        }
        else
        {
            logger.warn( "Ignoring unrecognized line: " +  line );
            return;
        }

        // If the file isn't a file; don't add it.
        if (files.isEmpty() || status == null) {
            return;
        }
        File checkDir = repositoryRoot;
        if (workingDirectory != null && relativeRepositoryPath != null) {
            // Make all paths relative to this directory.
            List<String> relativeNames = new ArrayList<String>();
            for (String repoRelativeName : files) {
                relativeNames.add(ScmSyncGitUtils.relativizePath(relativeRepositoryPath, new File(repoRelativeName).getPath()));
            }
            files = relativeNames;
            checkDir = workingDirectory;
        }
        // Now check them all against the checkDir. This check has been taken over from the base implementation
        // in maven-scm's GitStatusConsumer, but I'm not really sure this makes sense. Who said the workspace
        // had to be equal to the git index (staging area) here?
        if (status == ScmFileStatus.RENAMED) {
            String oldFilePath = files.get( 0 );
            String newFilePath = files.get( 1 );
            if (new File(checkDir, oldFilePath).isFile()) {
                logger.debug("file '" + oldFilePath + "' still exists after rename");
                return;
            }
            if (!new File(checkDir, newFilePath).isFile()) {
                logger.debug("file '" + newFilePath + "' does not exist after rename");
                return;
            }
        } else if (status == ScmFileStatus.DELETED) {
            if (new File(checkDir, files.get(0)).isFile()) {
                return;
            }
        } else {
            if (!new File(checkDir, files.get(0)).isFile()) {
                return;
            }
        }

        for (String file : files) {
            changedFiles.add(new ScmFile(file.replace(File.separatorChar, '/'), status));
        }
    }

    public List<ScmFile> getChangedFiles()
    {
        return changedFiles;
    }
}
