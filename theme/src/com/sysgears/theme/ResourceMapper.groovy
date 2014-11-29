package com.sysgears.theme

import com.sysgears.grain.taglib.Site

/**
 * Change pages urls and extend models.
 */
class ResourceMapper {

    /**
     * Site reference, provides access to site configuration.
     */
    private final Site site

    public ResourceMapper(Site site) {
        this.site = site
    }

    /**
     * This closure is used to transform page URLs and page data models.
     */
    def map = { resources ->

        def refinedResources = resources.findResults(filterPublished).collect { Map resource ->
            customizeUrls << fillDates << resource
        }

        refinedResources
    }

    /**
     * Customize site post URLs
     */
    private def customizeUrls = { Map resource ->
        String location = resource.location
        def update = [:]

        switch (location) {
            case ~/\/articles\/.*/:
                update.url = getPostUrl('/articles/', location)
                break
        }

        resource + update
    }

    /**
     * Creates url for page. Cuts date and extension from the file name '2013-01-01-file-name.markdown'.
     *
     * @param basePath base path to the page
     * @param location location of the file
     *
     * @return formatted url to the page.
     */
    private String getPostUrl(String basePath, String location) {
        basePath + location.substring(location.lastIndexOf('/') + 12, location.lastIndexOf('.')) + '/'
    }

    /**
     * Excludes resources with published property set to false,
     * unless it is allowed to show unpublished resources in SiteConfig.
     */
    private def filterPublished = { Map it ->
        (it.published != false || site.show_unpublished) ? it : null
    }

    /**
     * Fills in page `date` and `updated` fields 
     */
    private def fillDates = { Map it ->
        def update = [date: it.date ? Date.parse(site.datetime_format, it.date) : new Date(it.dateCreated as Long),
                updated: it.updated ? Date.parse(site.datetime_format, it.updated) : new Date(it.lastUpdated as Long)]
        it + update
    }
}
