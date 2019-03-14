package com.github.adminfaces.addon.facet;

import static com.github.adminfaces.addon.util.AdminScaffoldUtils.getService;
import static com.github.adminfaces.addon.util.Constants.WebResources.INCLUDES;
import static com.github.adminfaces.addon.util.Constants.WebResources.TEMPLATE_DEFAULT;
import static com.github.adminfaces.addon.util.Constants.WebResources.TEMPLATE_TOP;
import static com.github.adminfaces.addon.util.DependencyUtil.ADMIN_TEMPLATE_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.ADMIN_THEME_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.PRIMEFACES_EXTENSIONS_COORDINATE;

import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;

import com.github.adminfaces.addon.util.DependencyUtil;

/**
 * The implementation of the {@link AdminFacesFacet}
 *
 * @author <a href="mailto:rmpestano@gmail.com">Rafael Pestano</a>
 */
public class AdminFacesFacetImpl extends AbstractFacet<Project> implements AdminFacesFacet {


    @Override
    public boolean install() {
        addAdminFacesDependencies();
        return isInstalled();
    }

    private void addAdminFacesDependencies() {
    	DependencyUtil dependencyUtil = getService(DependencyUtil.class);
        DependencyFacet dependencyFacet = getFaceted().getFacet(DependencyFacet.class);
        DependencyBuilder adminTemplateDependency = DependencyBuilder.create()
            .setCoordinate(dependencyUtil.getLatestVersion(ADMIN_TEMPLATE_COORDINATE));
        DependencyBuilder adminThemeDependency = DependencyBuilder.create()
            .setCoordinate(dependencyUtil.getLatestVersion(ADMIN_THEME_COORDINATE));
        DependencyBuilder primefacesExtensionsDependency = DependencyBuilder.create()
            .setCoordinate(PRIMEFACES_EXTENSIONS_COORDINATE);
        dependencyUtil.installDependency(dependencyFacet, adminThemeDependency);
        dependencyUtil.installDependency(dependencyFacet, adminTemplateDependency);
        dependencyUtil.installDependency(dependencyFacet, primefacesExtensionsDependency);//only for gravatar

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
