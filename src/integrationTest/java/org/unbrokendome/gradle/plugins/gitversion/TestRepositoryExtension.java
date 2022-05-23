package org.unbrokendome.gradle.plugins.gitversion;

import org.spockframework.runtime.InvalidSpecException;
import org.spockframework.runtime.extension.IAnnotationDrivenExtension;
import org.spockframework.runtime.model.FieldInfo;

public class TestRepositoryExtension implements IAnnotationDrivenExtension<TestRepository> {

    @Override
    public void visitFieldAnnotation(TestRepository annotation, FieldInfo field) {
        var className = GitRepository.class.getSimpleName();

        if (!field.getType().isAssignableFrom(GitRepository.class)) {
            throw new InvalidSpecException("@TestRepository can only be used on a " + className + " field");

        } else {

            var interceptor = new TestRepositoryInterceptor(field);

            var specInfo = field.getParent();
            if (field.isShared()) {
                specInfo.getBottomSpec().addInterceptor(interceptor);
            } else {
                specInfo.getBottomSpec().getAllFeatures().forEach(featureInfo -> featureInfo.addInterceptor(interceptor));
            }

        }
    }

}
