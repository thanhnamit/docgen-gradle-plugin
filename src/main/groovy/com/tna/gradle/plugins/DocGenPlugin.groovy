package com.tna.gradle.plugins

import com.tna.gradle.extensions.DocGenExtension
import org.asciidoctor.gradle.jvm.AsciidoctorJPlugin
import org.asciidoctor.gradle.jvm.AsciidoctorTask
import org.asciidoctor.gradle.jvm.slides.AsciidoctorJRevealJSTask
import org.asciidoctor.gradle.jvm.slides.AsciidoctorRevealJSPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin
import com.github.mustachejava.DefaultMustacheFactory

class DocGenPlugin implements Plugin<Project> {
    public static final DOC_GEN = "docGen"
    public static final DOC_GROUP = 'documentation'
    public static final String ASCIIDOC_DIAGRAM_VER = '1.5.18'
    public static final String ASCIIDOC_GROOVYDSL_VER = '1.6.0'
    private mustacheFactory = new DefaultMustacheFactory()

    @Override
    void apply(Project project) {
        applyPlugins(project)
        updateConfigurations(project)

        addRepositories(project)
        addDependencies(project)

        def docGenExt = getDocGenExt(project)

        addAsciidocJExtension(project)
        addRevealJsExtension(project)
        addRevealJsPluginsExtension(project)

        addDocGenSlideTask(project, docGenExt, 'docGenSlide')
        addDocGenHtmlTask(project, docGenExt, 'docGenHtml')
        addDocGenDirTemplateTask(project, docGenExt, 'docGenDirTemplate')
    }

    def getDocGenExt(Project project) {
        def docGenExt = project.extensions.create(DOC_GEN, DocGenExtension.class)
        docGenExt
    }

    def addDocGenDirTemplateTask(Project project, DocGenExtension ext, String taskName) {
        project.task(taskName).configure {
            group DOC_GROUP
            description 'Generate document templates in asciidoc'
            doLast {
                def docSrc = "$ext.documentRoot/docs/asciidoc"
                def presentationSrc = "$docSrc/presentation"
                def specSrc = "$docSrc/specification"
                def staticSrc = "$docSrc/statics"
                project.mkdir(presentationSrc)
                project.mkdir(specSrc)
                project.mkdir(staticSrc)

                def bindings = getTemplateBindings(ext, project)
                def tmplFiles = getTemplateMaps(specSrc, presentationSrc, staticSrc)

                tmplFiles.each { k, v ->
                    def dest = new File(v)
                    def template = this.getClass().getResourceAsStream("/mustache/$k")
                    def mustache = mustacheFactory.compile(template.newReader(), k)
                    mustache.execute(dest.newWriter(), bindings).flush()
                }
            }
        }
    }

    def getTemplateMaps(GString specSrc, GString presentationSrc, GString staticSrc) {
        def tmplFiles = [
                "specification.adoc.mustache"           : "$specSrc/specification.adoc",
                "presentation.adoc.mustache"            : "$presentationSrc/presentation.adoc",
                "slide-plugin-configuration.js.mustache": "$staticSrc/slide-plugin-configuration.js",
                "LICENSE.mit.mustache"                  : 'LICENSE',
                "README.adoc.mustache"                  : 'README.adoc',
                "CHANGELOG.adoc.mustache"               : 'CHANGELOG.adoc',
                "CONTRIBUTING.adoc.mustache"            : 'CONTRIBUTING.adoc',
        ]
        tmplFiles
    }

    def getTemplateBindings(DocGenExtension ext, Project project) {
        def bindings = [
                'projectName'   : ext?.projectInfo?.projectName ?: project.group,
                'projectVersion': ext?.projectInfo?.projectVersion ?: project.version,
                'projectAuthors': ext?.projectInfo?.projectAuthors ?: 'N/A',
                'contactEmail'  : ext?.projectInfo?.contactEmail ?: 'N/A',
                'licenceYear'   : new Date().format('yyyy'),
                'documentDate'  : new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        ]
        bindings
    }

    def addDocGenHtmlTask(Project project, DocGenExtension ext, String taskName) {
        project.task(taskName, type: AsciidoctorTask) {
            group DOC_GROUP
            description 'Generate standard HTML documentation for asciidoc'
            dependsOn project.tasks['asciidoctorGemsPrepare']
            configurations 'asciidoctorExt'
            sourceDir project.file("$ext.documentRoot/docs/asciidoc/specification")
            outputDir "$project.buildDir/docs/asciidoc/specification"
        }
    }

    def addDocGenSlideTask(Project project, DocGenExtension ext, String docGenSlideTaskName) {
        project.task(docGenSlideTaskName, type: AsciidoctorJRevealJSTask) {
            group DOC_GROUP
            description 'Generate Reveal.JS presentation from asciidoc'
            dependsOn project.tasks['asciidoctorGemsPrepare']
            configurations 'asciidoctorExt'
            sourceDir project.file("$ext.documentRoot/docs/asciidoc/presentation")
            attributes (
                    'sourcedir': project.sourceSets.main.groovy.srcDirs[0],
                    'imagesDir': 'images',
                    'icons': 'font',
                    'iconfont-name': 'fontawesome-5.8.0'
            )
            revealjsOptions {
                controls = true
                slideNumber = true
                progressBar = true
                pushToHistory = true
                overviewMode = true
                touchMode = true
                //none , fade, slide, convex, concave, zoom
                backgroundTransition = 'slide'
                //'black', 'beige' , 'league', 'night', 'serif', 'simple', 'sky', 'solarized'
                theme = 'league'
            }

            plugins 'rajgoel/chart/Chart.min.js',
                    'rajgoel/chart/csv2chart.js',
                    'rajgoel/chalkboard/chalkboard.js',
                    'rajgoel/embed-tweet/embed-tweet.js',
                    'rajgoel/spreadsheet/spreadsheet.js',
                    'denehyg/menu.js'

            pluginConfigurationFile "$ext.documentRoot/docs/asciidoc/statics/slide-plugin-configuration.js"
            outputDir "$project.buildDir/docs/asciidoc/presentation"
        }
    }

    def addDependencies(Project project) {
        project.dependencies {
            implementation 'org.codehaus.groovy:groovy-all:2.5.7'
            implementation 'org.asciidoctor:asciidoctorj-groovy-dsl:1.6.0'
            implementation 'org.asciidoctor:asciidoctor-gradle-jvm:2.2.0'
            implementation 'org.asciidoctor:asciidoctor-gradle-jvm-slides:2.2.0'
            asciidoctorGems 'rubygems:asciidoctor-revealjs:1.1.3'
            asciidoctorExt 'net.sourceforge.plantuml:plantuml:1.2019.4'
        }
    }

    def addRepositories(Project project) {
        project.repositories {
            maven { url 'http://rubygems-proxy.torquebox.org/releases' }
            maven { url 'https://plugins.gradle.org/m2/' }
            mavenCentral()
            jcenter()
        }
    }

    def updateConfigurations(Project project) {
        project.configurations {
            asciidoctorExt
        }
    }

    def addRevealJsExtension(Project project) {
        project.revealjs {
            templateGitHub {
                organisation = 'hakimel'
                repository = 'reveal.js'
                tag = '3.7.0'
            }
        }
    }

    def addRevealJsPluginsExtension(Project project) {
        project.revealjsPlugins {
            github('denehyg') {
                organisation = 'denehyg'
                repository = 'reveal.js-menu'
                branch = 'master'
            }
            github('rajgoel') {
                organisation = 'rajgoel'
                repository = 'reveal.js-plugins'
                branch = 'master'
            }
        }
    }

    def addAsciidocJExtension(Project project) {
        project.asciidoctorj {
            modules {
                diagram {
                    version ASCIIDOC_DIAGRAM_VER
                }
                groovyDsl {
                    version ASCIIDOC_GROOVYDSL_VER
                }
            }
            docExtensions {
                block_macro(name: 'tweet') { parent, target, attributes ->
                    String content = """<div class="tweet" data-src="https://twitter.com/${target}/status/${
                        attributes.get('1')
                    }"></div>"""
                    config.remove 'content_model'
                    createBlock(parent, "pass", [content], [:], config)
                }

                block(name: 'spreadsheet', contexts: [':listing']) { parent, reader, attributes ->
                    def content = ['<div class="spreadsheet" data-delimiter="|">']
                    content.addAll reader.readLines()
                    content.add '</div>'
                    createBlock(parent, 'pass', content, attributes, [:])
                }

                postprocessor {
                    document, output ->
                        output = output.replace("<body>", """
                        <script src="reveal.js/plugin/rajgoel/spreadsheet/ruleJS.all.full.min.js"></script>
                        <link rel="stylesheet" href="reveal.js/plugin/rajgoel/spreadsheet/spreadsheet.css">
                        <body>
                        """)
                        output
                }
            }
        }
    }

    def applyPlugins(Project project) {
        project.plugins.apply(JavaGradlePluginPlugin)
        project.plugins.apply(GroovyPlugin)
        project.plugins.apply(AsciidoctorJPlugin)
        project.plugins.apply(AsciidoctorRevealJSPlugin)
        project.plugins.apply(MavenPublishPlugin)
    }
}
