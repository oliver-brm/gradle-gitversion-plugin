package org.unbrokendome.gradle.plugins.gitversion;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.spockframework.runtime.SpockExecutionException;
import org.spockframework.runtime.extension.IMethodInterceptor;
import org.spockframework.runtime.extension.IMethodInvocation;
import org.spockframework.runtime.model.FieldInfo;

import java.io.IOException;
import java.nio.file.Files;

public class TestRepositoryInterceptor implements IMethodInterceptor {

    private final FieldInfo fieldInfo;
    private final String tempDirPrefix = TestRepositoryInterceptor.class.getSimpleName();

    public TestRepositoryInterceptor(FieldInfo fieldInfo) {
        this.fieldInfo = fieldInfo;
    }

    GitRepository setUp(IMethodInvocation invocation) {
        try {
            var baseDir = Files.createTempDirectory(tempDirPrefix);
            var gitRepository = new GitRepository(baseDir.toFile());
            gitRepository.initialize();
            fieldInfo.writeValue(invocation.getInstance(), gitRepository);
            return gitRepository;
        } catch (GitAPIException | IOException e) {
            throw new SpockExecutionException("Cannot initialize GitRepository", e);
        }
    }

    @Override
    public void intercept(IMethodInvocation invocation) throws Throwable {
        var gitRepository = setUp(invocation);
        try {
            invocation.proceed();
        } finally {
            destroy(gitRepository);
        }
    }

    private void destroy(GitRepository gitRepository) {
        gitRepository.cleanUp();
    }

}
