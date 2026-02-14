package io.github.nickid2018.mock;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GenerateMockPlugin implements Plugin<Project> {
    @Override
    public void apply(Project target) {
        target.getExtensions().add(Mocker.class, "mocker", new Mocker(target));
    }
}
