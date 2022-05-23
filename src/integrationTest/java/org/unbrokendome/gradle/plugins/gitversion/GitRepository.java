package org.unbrokendome.gradle.plugins.gitversion;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public final class GitRepository {

    /**
     * root directory where all repositories (remote and clones) are stored
     */
    private final File rootDir;

    /** The directory where the (bare) repository is stored; this will serve as the remote */
    private File remoteRepositoryDir;

    /**
     * The working directory where the repository is checked out; this will be available to the test
     */
    private File workingDir;

    /**
     * The Git object for the remote repository.
     */
    private Git remoteGit;

    /**
     * The Git object for setting up the repository contents
     */
    private Git setupGit;
    /**
     * The Git object for the working dir
     */
    private Git git;

    public GitRepository(File rootDir) {
        this.rootDir = rootDir;
    }

    /**
     * Initializes the remote and local repositories.
     * To be called <strong>before</strong> {@link GitRepository#setup(ThrowingConsumer)}!
     * @throws GitAPIException if the remote or local repository could not be created
     */
    void initialize() throws GitAPIException {
        remoteRepositoryDir = new File(rootDir, "repo");
        assert remoteRepositoryDir.mkdir() : "Cannot create " + remoteRepositoryDir.getAbsolutePath();
        workingDir = new File(rootDir, "working");
        assert workingDir.mkdir() : "Cannot create " + workingDir.getAbsolutePath();

        // initialize the bare repository
        remoteGit = Git.init()
                .setDirectory(remoteRepositoryDir)
                .setBare(true)
                .call();

        // clone the repository into the setup working dir
        var setupWorkingDir = new File(rootDir, "setup");
        assert setupWorkingDir.mkdir() : "Cannot create " + setupWorkingDir.getAbsolutePath();
        setupGit = Git.cloneRepository()
                .setDirectory(setupWorkingDir)
                .setURI(remoteRepositoryDir.toURI().toString())
                .call();
    }

    /**
     * To be called to close all resources.
     */
    void cleanUp() {
        git.close();
        setupGit.close();
        remoteGit.close();
    }

    /**
     * Like a {@link java.util.function.Consumer}, but declaring that it throws {@link Exception}, so the API user
     * does not need to clutter the code with {@code try-catch} blocks.
     * @param <T> whatever it is that this Consumer consumes
     */
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    public void setup(ThrowingConsumer<GitBuilder> action) throws Exception {

        GitBuilder builder = new GitBuilder(setupGit);
        action.accept(builder);

        // push all local branches to origin
        List<Ref> localBranches = setupGit.branchList().call();
        PushCommand pushCommand = setupGit.push()
                .setRemote("origin");
        localBranches.forEach(pushCommand::add);
        pushCommand.call();
    }


    public File cloneAndCheckout(String branchName) throws GitAPIException {
        git = Git.cloneRepository()
                .setDirectory(workingDir)
                .setURI(remoteRepositoryDir.toURI().toString())
                .setBranch(branchName)
                .call();

        return workingDir;
    }


    public void detach() throws GitAPIException, IOException {
        ObjectId headId = git.getRepository().resolve("HEAD");
        git.checkout()
                .setName(headId.name())
                .call();
    }


    public File getWorkingDir() {
        return workingDir;
    }


    public Git getGit() {
        return git;
    }


    static class GitBuilder {

        private final Git git;
        private final Path workingDir;


        GitBuilder(Git git) {
            this.git = git;
            this.workingDir = git.getRepository().getWorkTree().toPath();
        }


        public GitBuilder commitFile(String fileName, String commitMessage, @Nullable String fileContents)
                throws Exception {

            Path path = workingDir.resolve(fileName);
            if (fileContents != null) {
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    writer.write(fileContents);
                }
            }
            git.add()
                    .addFilepattern(fileName)
                    .call();
            git.commit()
                    .setMessage(commitMessage)
                    .call();
            return this;
        }

        public GitBuilder commitFile(String fileName, String commitMessage) throws Exception {
            return commitFile(fileName, commitMessage, null);
        }

        public GitBuilder checkout(String branch) throws Exception {
            git.checkout()
                    .setName(branch)
                    .call();
            return this;
        }

        public GitBuilder checkoutNew(String branch) throws Exception {
            git.checkout()
                    .setName(branch)
                    .setCreateBranch(true)
                    .call();
            return this;
        }

        public GitBuilder merge(String fromBranch, @Nullable String message) throws Exception {
            git.merge()
                    .include(git.getRepository().findRef(fromBranch))
                    .setFastForward(MergeCommand.FastForwardMode.NO_FF)
                    .setCommit(true)
                    .setMessage(message)
                    .call();
            return this;
        }

        public GitBuilder merge(String fromBranch) throws Exception {
            return merge(fromBranch, null);
        }

        public GitBuilder tag(String tagName) throws Exception {
            git.tag()
                    .setName(tagName)
                    .call();
            return this;
        }
    }
}
