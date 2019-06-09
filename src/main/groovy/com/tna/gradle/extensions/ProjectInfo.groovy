package com.tna.gradle.extensions

import lombok.AllArgsConstructor
import lombok.Data

@Data
@AllArgsConstructor
class ProjectInfo {
    String projectName
    String projectVersion
    String projectAuthors
    String contactEmail
}
