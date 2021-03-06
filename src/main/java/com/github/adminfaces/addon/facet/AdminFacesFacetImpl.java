package com.github.adminfaces.addon.facet;

import com.github.adminfaces.addon.util.DependencyUtil;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;

import javax.inject.Inject;

import static com.github.adminfaces.addon.util.DependencyUtil.*;
import static com.github.adminfaces.addon.util.Constants.WebResources.*;

/**
 * The implementation of the {@link AdminFacesFacet}
 *
 * @author <a href="mailto:rmpestano@gmail.com">Rafael Pestano</a>
 */
public class AdminFacesFacetImpl extends AbstractFacet<Project> implements AdminFacesFacet {

    @Inject
    private DependencyUtil dependencyUtil;

    @Override
    public boolean install() {
        addAdminFacesDependencies();
        return isInstalled();
    }

    private void addAdminFacesDependencies() {
        DependencyFacet dependencyFacet = getFaceted().getFacet(DependencyFacet.class);
        DependencyBuilder adminTemplateDependency = DependencyBuilder.create()
            .setCoordinate(dependencyUtil.getLatestVersion(ADMIN_TEMPLATE_COORDINATE));
        DependencyBuilder adminThemeDependency = DependencyBuilder.create()
            .setCoordinate(dependencyUtil.getLatestVersion(ADMIN_THEME_COORDINATE));
        DependencyBuilder primefacesExtensionsDependency = DependencyBuilder.create()
            .setCoordinate(PRIMEFACES_EXTENSIONS_COORDINATE);
        DependencyBuilder omniFacesDependency = DependencyBuilder.create()
                .setCoordinate(OMNIFACES_COORDINATE);

        dependencyUtil.installDependency(dependencyFacet, adminThemeDependency);
        dependencyUtil.installDependency(dependencyFacet, adminTemplateDependency);
        dependencyUtil.installDependency(dependencyFacet, primefacesExtensionsDependency);//only for gravatar
        dependencyUtil.installDependency(dependencyFacet, omniFacesDependency);
    }

    @Override
    public boolean isInstalled() {
        DependencyFacet facet = getFaceted().getFacet(DependencyFacet.class);
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(ADMIN_TEMPLATE_COORDINATE.getArtifactId())
            .setGroupId(ADMIN_TEMPLATE_COORDINATE.getGroupId()))
            && isApplicationTemplateInstalled() && isMenusInstalled();
    }

    private boolean isMenusInstalled() {
        WebResourcesFacet web = getFaceted().getFacet(WebResourcesFacet.class);

        return web.getWebResource(INCLUDES + "/menu.xhtml").exists()
            && web.getWebResource(INCLUDES + "/menubar.xhtml").exists();
    }

    private boolean isApplicationTemplateInstalled() {
        WebResourcesFacet web = getFaceted().getFacet(WebResourcesFacet.class);

        return web.getWebResource(TEMPLATE_DEFAULT).exists() && web.getWebResource(TEMPLATE_TOP).exists();
    }

}
