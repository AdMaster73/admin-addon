package com.github.adminfaces.addon.ui;

import static org.jboss.forge.addon.scaffold.util.ScaffoldUtil.createOrOverwrite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.javaee.cdi.CDIFacet;
import org.jboss.forge.addon.javaee.cdi.CDIFacet_1_0;
import org.jboss.forge.addon.javaee.faces.FacesFacet;
import org.jboss.forge.addon.javaee.faces.FacesFacet_2_0;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.javaee.servlet.ServletFacet_3_1;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.resources.MavenModelResource;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.shrinkwrap.descriptor.api.javaee7.ParamValueType;
import org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor;
import org.jboss.shrinkwrap.descriptor.api.webcommon31.ServletMappingType;
import org.metawidget.util.simple.StringUtils;

import com.github.adminfaces.addon.freemarker.FreemarkerTemplateProcessor;
import com.github.adminfaces.addon.freemarker.TemplateFactory;

import static com.github.adminfaces.addon.util.Constants.WebResources.*;
import com.github.adminfaces.addon.facet.AdminFacesFacet;
import java.io.ByteArrayInputStream;

/**
 * AdminFaces: Setup command
 *
 * @author <a href="mailto:rmpestano@gmail.com">Rafael Pestano</a>
 */
@FacetConstraint({JavaEE7Facet.class, WebResourcesFacet.class})
public class AdminSetupCommand extends AbstractProjectCommand {

    private static final Logger LOG = Logger.getLogger(AdminSetupCommand.class.getName());

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private TemplateFactory templates;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass()).name("AdminFaces: Setup").category(Categories.create("AdminFaces"))
            .description("Setup AdminFaces dependencies in the current project.");
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final Project project = getSelectedProject(context) != null ? getSelectedProject(context)
            : getSelectedProject(context.getUIContext());
        boolean execute = true;
        if (project.hasFacet(AdminFacesFacet.class) && project.getFacet(AdminFacesFacet.class).isInstalled()) {
            execute = context.getPrompt().promptBoolean("AdminFaces is already installed, override it?");
        }

        if (!execute) {
            return Results.success();
        }
        List<Result> results = new ArrayList<>();
        AdminFacesFacet facet = facetFactory.create(project, AdminFacesFacet.class);
        facetFactory.install(project, facet);
        results.add(Results.success("AdminFaces setup completed successfully!"));

        if (!project.hasFacet(ServletFacet_3_1.class)) {
            ServletFacet_3_1 servletFacet_3_1 = facetFactory.create(project, ServletFacet_3_1.class);
            facetFactory.install(project, servletFacet_3_1);
        }
        if (!project.hasFacet(FacesFacet_2_0.class)) {
            FacesFacet_2_0 facesFacet = facetFactory.create(project, FacesFacet_2_0.class);
            facetFactory.install(project, facesFacet);
        }
        if (!project.hasFacet(CDIFacet.class)) {
            CDIFacet_1_0 cdiFacet = facetFactory.create(project, CDIFacet_1_0.class);
            facetFactory.install(project, cdiFacet);
        }

        updatePom(project);
        
        
        
        addAdminFacesResources(project).forEach(r -> results.add(Results
            .success("Added " + r.getFullyQualifiedName().replace(project.getRoot().getFullyQualifiedName(), ""))));
        setupWebXML(project);

        return Results.aggregate(results);

    }

	private void updatePom(final Project project) {
		MavenFacet m2 = project.getFacet(MavenFacet.class);
        MavenModelResource m2Model = m2.getModelResource();
        Node node = XMLParser.parse(m2Model.getResourceInputStream());
        
        addResourceFiltering(m2Model, node);
        addFinalName(m2Model, node, project);
	}

	/**
	 * If finalName doesn't exists then adds it using project artifactId
	 * @param m2Model
	 * @param root
	 * @param project
	 */
	private void addFinalName(MavenModelResource m2Model, Node root, Project project) {
		if(root.getOrCreate("build").get("finalName").isEmpty()) {
			MavenFacet maven = project.getFacet(MavenFacet.class);
	        String artifactId = maven.getModel().getArtifactId();
			root.getSingle("build")
					 .createChild("finalName")
					 .text(artifactId);
			 m2Model.setContents(XMLParser.toXMLInputStream(root));
		}
	}

	private void addResourceFiltering(MavenModelResource m2Model, Node root) {
		Node resourcesNode = root.getOrCreate("build").getOrCreate("resources");
        Optional<Node> resourcesFiltering = resourcesNode.get("resource").stream()
            .filter(r -> r.getName().equals("directory") && r.getText().equals("src/main/resources")).findFirst();
        if (!resourcesFiltering.isPresent()) {
            Node resource = resourcesNode.createChild("resource");
            resource.createChild("filtering").text("true");
            resource.createChild("directory").text("src/main/resources");
            m2Model.setContents(XMLParser.toXMLInputStream(root));
        }
	}

    private String resolveLogoMini(String projectName) {
        if (projectName.length() > 3) {
            return projectName.substring(0, 3);
        } else {
            return projectName;
        }
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
    }

    @SuppressWarnings("rawtypes")
    protected List<Resource<?>> addAdminFacesResources(Project project) {
        List<Resource<?>> result = new ArrayList<>();
        WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
        JavaSourceFacet javaSource = project.getFacet(JavaSourceFacet.class);
        ServletFacet_3_1 servlet = project.getFacet(ServletFacet_3_1.class);

        org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor servletConfig = (org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor) servlet
            .getConfig();
        servletConfig.getOrCreateWelcomeFileList().welcomeFile(INDEX_HTML);
        HashMap<Object, Object> context = getTemplateContext();
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        String projectName = metadataFacet.getProjectName();
        String appName = StringUtils.uncamelCase(projectName.replaceAll("-", " "));
        String logoMini = resolveLogoMini(appName);
        context.put("appName", appName);
        context.put("logoMini", logoMini);
        context.put("copyrightYear", Year.now().toString());
        // admin config
        addAdminConfig(project, result);
        // Basic pages
        if (!web.getWebResource(INDEX_PAGE).exists()) {
            result.add(createOrOverwrite(web.getWebResource(INDEX_PAGE),
                FreemarkerTemplateProcessor.processTemplate(context, templates.getIndexTemplate())));
        }
        if (!web.getWebResource(LOGIN_PAGE).exists()) {
            result.add(createOrOverwrite(web.getWebResource(LOGIN_PAGE),
                FreemarkerTemplateProcessor.processTemplate(context, templates.getLoginTemplate())));
        }

        // page templates
        result.add(createOrOverwrite(web.getWebResource(TEMPLATE_DEFAULT),
            FreemarkerTemplateProcessor.processTemplate(context, templates.getTemplateDefault())));
        result.add(createOrOverwrite(web.getWebResource(TEMPLATE_TOP),
            FreemarkerTemplateProcessor.processTemplate(context, templates.getTemplateTop())));

        // menus
        result.add(createOrOverwrite(web.getWebResource(INCLUDES + "/menu.xhtml"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + INCLUDES + "/menu.xhtml")));
        result.add(createOrOverwrite(web.getWebResource(INCLUDES + "/menubar.xhtml"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + INCLUDES + "/menubar.xhtml")));
        result.add(createOrOverwrite(web.getWebResource(INCLUDES + "/top-bar.xhtml"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + INCLUDES + "/top-bar.xhtml")));
        result.add(createOrOverwrite(web.getWebResource(INCLUDES + "/controlsidebar-tabs-content.xhtml"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + INCLUDES + "/controlsidebar-tabs-content.xhtml")));
        if (!web.getWebResource("WEB-INF/beans.xml").exists()) {
            result.add(createOrOverwrite(web.getWebResource("WEB-INF/beans.xml"),
                getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/WEB-INF/beans.xml")));
        }

        // beans
        try (InputStream logonStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("/infra/security/LogonMB.java")) {
            JavaSource<?> logonMB = (JavaSource<?>) Roaster.parse(logonStream);
            logonMB.setPackage(metadataFacet.getProjectGroupName() + ".infra");
            javaSource.saveJavaSource(logonMB);
            FileUtils.copyInputStreamToFile(logonStream,
                new File(project.getRoot().getFullyQualifiedName() + logonMB.getPackage().replaceAll("\\.", "/")));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not add 'LogonMB'.", e);
        }

        // Static resources
        createOrOverwrite(web.getWebResource("/resources/favicon/favicon.ico"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/favicon.ico"));
        createOrOverwrite(web.getWebResource("/resources/favicon/favicon-16x16.png"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/favicon-16x16.png"));
        createOrOverwrite(web.getWebResource("/resources/favicon/favicon-32x32.png"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/favicon-32x32.png"));
        createOrOverwrite(web.getWebResource("/resources/favicon/favicon-96x96.png"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/favicon-96x96.png"));
        createOrOverwrite(web.getWebResource("/resources/images/login-bg.jpg"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/login-bg.jpg"));
        createOrOverwrite(web.getWebResource("/resources/images/login-bg-mobile.jpeg"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/login-bg-mobile.jpeg"));
        createOrOverwrite(web.getWebResource("/resources/css/app.css"),
            getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/css/app.css"));
        setupDocker(project, result);
        return result;
    }

    private void addAdminConfig(Project project, List<Resource<?>> result) {

        Resource<?> resources = project.getRoot().reify(DirectoryResource.class).getChildDirectory("src")
            .getChildDirectory("main").getOrCreateChildDirectory("resources");

        if (!resources.getChild("admin-config.properties").exists()) {
            try {
                IOUtils.copy(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream("/admin-config.properties"),
                    new FileOutputStream(new File(resources.getFullyQualifiedName() + "/admin-config.properties")));
                result.add(resources.getChild("admin-config.properties"));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Could not add 'admin-config.properties'.", e);
            }
        }

        if (!resources.getChild("messages.properties").exists()) {
            try {
                IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("/messages.properties"),
                    new FileOutputStream(new File(resources.getFullyQualifiedName() + "/messages.properties")));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Could not add 'admin-config.properties'.", e);
            }
        }

    }

    protected void setupWebXML(Project project) {
        ServletFacet_3_1 servlet = project.getFacet(ServletFacet_3_1.class);
        WebAppDescriptor servletConfig = (WebAppDescriptor) servlet.getConfig();
        final List<ParamValueType<WebAppDescriptor>> allContextParam = servletConfig.getAllContextParam();
        setWebXmlContextParam(servletConfig, allContextParam, "javax.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE", "true");
        setWebXmlContextParam(servletConfig, allContextParam, "javax.faces.INTERPRET_EMPTY_STRING_SUBMITTED_VALUES_AS_NULL", "true");
        configPrimeFaces(servletConfig, allContextParam);
        FacesFacet facesFacet = project.getFacet(FacesFacet.class);
        configOmniFaces(servletConfig, facesFacet);
        configNumberOfFacesViews(servletConfig, allContextParam);
        setupErrorPages(servletConfig);
        servlet.saveConfig(servletConfig);
    }

    private void setupErrorPages(WebAppDescriptor servletConfig) {
        String pageSuffix = ".xhtml";
        Optional<ServletMappingType<WebAppDescriptor>> facesServlet = servletConfig.getAllServletMapping().stream()
            .filter(m -> m.getServletName().equals("Faces Servlet")).findFirst();
        if (facesServlet.isPresent() && !facesServlet.get().getAllUrlPattern().isEmpty()) {
            String urlMapping = facesServlet.get().getAllUrlPattern().get(0);
            if (urlMapping.contains(".")) {
                pageSuffix = facesServlet.get().getAllUrlPattern().get(0).substring(urlMapping.indexOf("."));
            }
        }

        if (!pageSuffix.endsWith(".xhtml")) {
            String errorCode = "401";
            if (!isErrorPageConfigured(servletConfig, errorCode)) {
                servletConfig.createErrorPage().errorCode(errorCode).location("/" + errorCode + pageSuffix);
            }

            errorCode = "403";
            if (!isErrorPageConfigured(servletConfig, errorCode)) {
                servletConfig.createErrorPage().errorCode(errorCode).location("/" + errorCode + pageSuffix);
                servletConfig.createErrorPage()
                    .exceptionType("com.github.adminfaces.template.exception.AccessDeniedException")
                    .location("/" + errorCode + pageSuffix);
            }

            errorCode = "404";
            if (!isErrorPageConfigured(servletConfig, errorCode)) {
                servletConfig.createErrorPage().errorCode(errorCode).location("/" + errorCode + pageSuffix);
            }

            errorCode = "500";
            if (!isErrorPageConfigured(servletConfig, errorCode)) {
                servletConfig.createErrorPage().errorCode(errorCode).location("/" + errorCode + pageSuffix);
                servletConfig.createErrorPage().exceptionType("java.lang.Throwable").location("/500" + pageSuffix);
            }

            String exceptionType = "javax.faces.application.ViewExpiredException";
            if (!isErrorPageExceptionTypeConfigured(servletConfig, exceptionType)) {
                servletConfig.createErrorPage().exceptionType(exceptionType).location("/expired" + pageSuffix);
            }

            exceptionType = "javax.persistence.OptimisticLockException";
            if (!isErrorPageExceptionTypeConfigured(servletConfig, exceptionType)) {
                servletConfig.createErrorPage().exceptionType(exceptionType).location("/optimistic" + pageSuffix);
            }
        }
    }

    private boolean isErrorPageConfigured(WebAppDescriptor servletConfig, String errorCode) {
        return servletConfig.getAllErrorPage().stream().filter(e -> errorCode.equals(e.getErrorCode())).count() > 0;
    }

    private boolean isErrorPageExceptionTypeConfigured(WebAppDescriptor servletConfig, String exceptionType) {
        return servletConfig.getAllErrorPage().stream().filter(e -> exceptionType.equals(e.getExceptionType()))
            .count() > 0;
    }

    private void configOmniFaces(WebAppDescriptor servletConfig, FacesFacet facesFacet) {
        boolean found;
        found = servletConfig.getAllFilter().stream()
            .filter(f -> f.getFilterClass().equals("org.omnifaces.filter.GzipResponseFilter")).findAny()
            .isPresent();

        if (!found) {
            servletConfig.createFilter().filterName("gzipResponseFilter")
                .filterClass("org.omnifaces.filter.GzipResponseFilter").createInitParam().paramName("threshold")
                .paramValue("200");
        }

        FileResource<?> configFile = facesFacet.getConfigFile();

        Node node = XMLParser.parse(configFile.getResourceInputStream());
        Node applicationNode = node.getOrCreate("application");
        Optional<Node> combinedResourceHandler = applicationNode.getChildren().stream()
            .filter(f -> f.getName().equals("resource-handler") && f.getText().contains("CombinedResourceHandler"))
            .findFirst();

        if (!combinedResourceHandler.isPresent()) {
            applicationNode.createChild("resource-handler")
                .text("org.omnifaces.resourcehandler.CombinedResourceHandler");

            configFile.setContents(XMLParser.toXMLInputStream(node));
        }

    }

    private void configPrimeFaces(WebAppDescriptor servletConfig,
        List<ParamValueType<WebAppDescriptor>> allContextParam) {
        setWebXmlContextParam(servletConfig, allContextParam, "primefaces.THEME", "admin", true);
        setWebXmlContextParam(servletConfig, allContextParam, "primefaces.FONT_AWESOME", "true", true);
        setWebXmlContextParam(servletConfig, allContextParam, "primefaces.MOVE_SCRIPTS_TO_BOTTOM", "true");
    }

    private void configNumberOfFacesViews(WebAppDescriptor servletConfig,
        List<ParamValueType<WebAppDescriptor>> allContextParam) {
        final String NUM_FACES_VIEWS = "6";
        setWebXmlContextParam(servletConfig, allContextParam, "com.sun.faces.numberOfLogicalViews", NUM_FACES_VIEWS);
        setWebXmlContextParam(servletConfig, allContextParam, "com.sun.faces.numberOfViewsInSession", NUM_FACES_VIEWS);
        setWebXmlContextParam(servletConfig, allContextParam, "org.omnifaces.VIEW_SCOPE_MANAGER_MAX_ACTIVE_VIEW_SCOPES", NUM_FACES_VIEWS);
    }

    private void setWebXmlContextParam(WebAppDescriptor servletConfig,
        List<ParamValueType<WebAppDescriptor>> allContextParam, String paramName, String paramValue) {

        setWebXmlContextParam(servletConfig, allContextParam, paramName, paramValue, false);
    }

    private void setWebXmlContextParam(WebAppDescriptor servletConfig,
        List<ParamValueType<WebAppDescriptor>> allContextParam, String paramName, String paramValue, boolean override) {

        Optional<ParamValueType<WebAppDescriptor>> contextParam = allContextParam.stream()
            .filter(c -> c.getParamName().equals(paramName)).findAny();

        if (!contextParam.isPresent()) {
            servletConfig.createContextParam().paramName(paramName)
                .paramValue(paramValue);
        } else if (override) {
            contextParam.get().paramValue(paramValue);
        }

    }

    private HashMap<Object, Object> getTemplateContext() {
        HashMap<Object, Object> context;
        context = new HashMap<>();
        context.put("templatePath", PAGE_TEMPLATE);
        return context;
    }

    private void setupDocker(Project project, List<Resource<?>> result) {
        DirectoryResource root = project.getRoot().reify(DirectoryResource.class);
        if (!root.getChild("Dockerfile").exists()) {
            MavenFacet maven = project.getFacet(MavenFacet.class);
            String artifactId = maven.getModel().getArtifactId();
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/scaffold/docker/Dockerfile")) {
                String dockerfileContent = IOUtils.toString(is, "UTF-8");
                dockerfileContent = dockerfileContent.replaceAll("ARTIFACTID", artifactId);
                IOUtils.copy(new ByteArrayInputStream(dockerfileContent.getBytes("UTF-8")),
                    new FileOutputStream(new File(root.getFullyQualifiedName() + "/Dockerfile")));
                result.add(root.getChild("Dockerfile"));
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Could not add 'Dockerfile'.", ex);
            }
            
            //add build and run script
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/scaffold/docker/build-and-run.sh")) {
                IOUtils.copy(is, new FileOutputStream(new File(root.getFullyQualifiedName() + "/build-and-run.sh")));
                result.add(root.getChild("build-and-run.sh"));
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Could not add 'build-and-run.sh'.", ex);
            }

            //create docker folder and put run.sh, build.sh utilities
            DirectoryResource dockerFolder = root.getOrCreateChildDirectory("docker");
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/scaffold/docker/build.sh")) {
                String dockerBuildContent = IOUtils.toString(is, "UTF-8");
                dockerBuildContent = dockerBuildContent.replaceAll("ARTIFACTID", artifactId.toLowerCase());
                IOUtils.copy(new ByteArrayInputStream(dockerBuildContent.getBytes("UTF-8")),
                    new FileOutputStream(new File(dockerFolder.getFullyQualifiedName() + "/build.sh")));
                result.add(dockerFolder.getChild("build.sh"));
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Could not add 'build.sh'.", ex);
            }
            
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/scaffold/docker/run.sh")) {
                String dockerRunContent = IOUtils.toString(is, "UTF-8");
                dockerRunContent = dockerRunContent.replaceAll("ARTIFACTID", artifactId.toLowerCase());
                IOUtils.copy(new ByteArrayInputStream(dockerRunContent.getBytes("UTF-8")),
                    new FileOutputStream(new File(dockerFolder.getFullyQualifiedName() + "/run.sh")));
                 result.add(dockerFolder.getChild("run.sh"));
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Could not add 'run.sh'.", ex);
            }
            
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("/scaffold/docker/standalone.conf")) {
                IOUtils.copy(is, new FileOutputStream(new File(dockerFolder.getFullyQualifiedName() + "/standalone.conf")));
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Could not add 'standalone.conf'.", ex);
            }
        }
    }

}
