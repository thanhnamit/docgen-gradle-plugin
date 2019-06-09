package com.tna.gradle.extensions

import lombok.Data
import org.gradle.api.Action

@Data
class DocGenExtension {
    String documentRoot = 'src'
    final ProjectInfo projectInfo = new ProjectInfo()
    def projectInfo(Action<? super ProjectInfo> action) {
        action.execute(projectInfo)
    }
}
