package com.sysgears.theme

import com.sysgears.grain.taglib.Site
import com.sysgears.theme.pagination.Paginator
import groovy.util.logging.Slf4j

/**
 * Change pages urls and extend models.
 */
@Slf4j
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

        def filterResources = filterKnownTypes(resources).findResults(filterPublished)

        def refinedResources = filterResources.collect { Map resource ->
            customizeUrls <<
                fillDates <<
                resource
        }.sort { -it.date.time }

        customizeModels << refinedResources
    }

    /**
     * Customizes pages models, applies pagination (creates new pages)
     */
    private def customizeModels = { List resources ->
        def posts = resources.findAll { it.layout == 'post' }

        resources.inject([]) { List updatedResources, Map page ->
            def applyPagination = { items, perPage, url, model = [:] ->
                updatedResources += Paginator.paginate(items, 'posts', perPage, url, page + model)
            }
            switch (page.url) {
                case '/blog/atom.xml':
                case '/blog/rss.xml':
                    def lastUpdated = posts.max { it.updated.time }.updated
                    def feedPosts = posts.take(site.blog_feed.posts_per_feed as Integer)
                    updatedResources << (page + [posts: feedPosts, lastUpdated: lastUpdated])
                    break
                case '/blog/':
                    applyPagination(posts, 3, page.url)
                    break
                case ~/${site.posts_base_url}.*/:
                    def post = posts.find { it.url == page.url }
                    def index = posts.indexOf(post)
                    def prev = index > 0 ? posts[index - 1] : null
                    def next = posts[index + 1]
                    updatedResources << (page + [prev_post: prev, next_post: next])
                    break
                default:
                    updatedResources << page
            }

            updatedResources
        }
    }

    /**
     * Customize site post URLs
     */
    private def customizeUrls = { Map resource ->
        String location = resource.location
        def update = [:]

        switch (location) {
            case ~/\/blog\/posts\/.*/:
                update.url = getPostUrl(site.posts_base_url, location)
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
    private static String getPostUrl(String basePath, String location) {
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

    private def filterKnownTypes = { resources ->
        def (knownResources, unknownResources) = resources.split { resource ->
            site.known_file_types.any { type -> resource.location.endsWith(type) }
        }
        unknownResources << handleUnknownResources
        knownResources
    }

    private def handleUnknownResources = { resources ->
        def locations = resources.collect { it -> it.location}
        log.warn "WARNING: you tried to use files with unknown type: ${locations.join(",\n")}.\n" +
                "These files wasn't handled to prevent unexpected errors.\n" +
                "If you really want to use this files, please extend the list of known file types by adding " +
                "needed file extensions to the \"known_file_types\" in the SiteConfig.groovy."
    }
}
