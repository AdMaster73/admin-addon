package com.github.adminfaces.addon.freemarker;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import freemarker.template.Template;

@ApplicationScoped
public class TemplateFactory implements Serializable {

    private static final String INDEX_TEMPLATE = "scaffold/freemarker/index.xhtml";

    private static final String LOGIN_TEMPLATE = "scaffold/freemarker/login.xhtml";

    private static final String TEMPLATE_TOP_TEMPLATE = "scaffold/freemarker/template-top.xhtml";

    private static final String TEMPLATE_DEFAULT_TEMPLATE = "scaffold/freemarker/template.xhtml";

    private static final String SERVICE_TEMPLATE = "scaffold/freemarker/Service.jv";

    private static final String REPOSITORY_TEMPLATE = "scaffold/freemarker/Repository.jv";

    private static final String LIST_MB_TEMPLATE = "scaffold/freemarker/ListMB.jv";

    private static final String FORM_MB_TEMPLATE = "scaffold/freemarker/FormMB.jv";
    
    private static final String LIST_PAGE_TEMPLATE = "scaffold/freemarker/list.xhtml";
    
    private static final String FORM_PAGE_TEMPLATE = "scaffold/freemarker/form.xhtml";

    private static final String DATASET_TEMPLATE = "scaffold/freemarker/dataset.yml";

    private static final String SERVICE_TEST_TEMPLATE = "scaffold/freemarker/ServiceTest.jv";

    private Template indexTemplate;

    private Template loginTemplate;

    private Template templateTop;

    private Template templateDefault;

    private Template serviceTemplate;

    private Template repositoryTemplate;

    private Template listMBTemplate;

    private Template formMBTemplate;
    
    private Template listPageTemplate;
    
    private Template formPageTemplate;

    private Template dataSetTemplate;

    private Template serviceTestTemplate;
    
    @PostConstruct
    public void initTemplates() {
        indexTemplate = FreemarkerTemplateProcessor.getTemplate(INDEX_TEMPLATE);
        templateTop = FreemarkerTemplateProcessor.getTemplate(TEMPLATE_TOP_TEMPLATE);
        templateDefault = FreemarkerTemplateProcessor.getTemplate(TEMPLATE_DEFAULT_TEMPLATE);
        loginTemplate = FreemarkerTemplateProcessor.getTemplate(LOGIN_TEMPLATE);
        serviceTemplate = FreemarkerTemplateProcessor.getTemplate(SERVICE_TEMPLATE);
        repositoryTemplate = FreemarkerTemplateProcessor.getTemplate(REPOSITORY_TEMPLATE);
        listMBTemplate = FreemarkerTemplateProcessor.getTemplate(LIST_MB_TEMPLATE);
        formMBTemplate = FreemarkerTemplateProcessor.getTemplate(FORM_MB_TEMPLATE);
        listPageTemplate = FreemarkerTemplateProcessor.getTemplate(LIST_PAGE_TEMPLATE);
        formPageTemplate = FreemarkerTemplateProcessor.getTemplate(FORM_PAGE_TEMPLATE);
    }


    public Template getIndexTemplate() {
        return indexTemplate;
    }

    public Template getTemplateTop() {
        return templateTop;
    }

    public Template getTemplateDefault() {
        return templateDefault;
    }

    public Template getLoginTemplate() {
        return loginTemplate;
    }

    public Template getServiceTemplate() {
        return serviceTemplate;
    }

    public Template getRepositoryTemplate() {
        return repositoryTemplate;
    }

    public Template getListMBTemplate() {
        return listMBTemplate;
    }

    public Template getFormMBTemplate() {
        return formMBTemplate;
    }

    public Template getListPageTemplate() {
        return listPageTemplate;
    }

    public Template getFormPageTemplate() {
        return formPageTemplate;
    }

    public Template getDataSetTemplate() {
        if(dataSetTemplate == null) {
            dataSetTemplate = FreemarkerTemplateProcessor.getTemplate(DATASET_TEMPLATE);
        }
        return dataSetTemplate;
    }

    public Template getServiceTestTemplate() {
        if(serviceTestTemplate == null) {
            serviceTestTemplate = FreemarkerTemplateProcessor.getTemplate(SERVICE_TEST_TEMPLATE);
        }
        return serviceTestTemplate;
    }
}
