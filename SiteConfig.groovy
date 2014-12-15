import com.sysgears.theme.ResourceMapper
import com.sysgears.theme.deploy.GHPagesDeployer
import com.sysgears.theme.taglib.ThemeTagLib

// Resource mapper and tag libs.
resource_mapper = new ResourceMapper(site).map
tag_libs = [ThemeTagLib]

excludes += ['/_[^/]*/.*'] // excludes directories that start from '_'

features {
    highlight = 'none' // 'none', 'pygments'
    compass = 'none'
    markdown = 'txtmark'   // 'txtmark', 'pegdown'
}

environments {
    dev {
        log.info 'Development environment is used'
        url = "http://localhost:${jetty_port}"
        show_unpublished = true
    }
    prod {
        log.info 'Production environment is used'
        url = '' // site URL, for example http://www.example.com
        show_unpublished = false
        features {
            minify_xml = false
            minify_html = false
            minify_js = false
            minify_css = false
        }
    }
    cmd {
        features {
            compass = 'none'
            highlight = 'none'
        }
    }
}

python {
    interpreter = 'jython' // 'auto', 'python', 'jython'
    //cmd_candidates = ['python2', 'python', 'python2.7']
    //setup_tools = '2.1'
}

ruby {
    interpreter = 'jruby'   // 'auto', 'ruby', 'jruby'
    //cmd_candidates = ['ruby', 'ruby1.8.7', 'ruby1.9.3', 'user.home/.rvm/bin/ruby']
    //ruby_gems = '2.2.2'
}

// Site configuration.
posts_base_url = '/blog/posts/' // the base url for blog entries

// Deployment settings.
s3_bucket = '' // your S3 bucket name
deploy_s3 = "s3cmd sync --acl-public --reduced-redundancy ${destination_dir}/ s3://${s3_bucket}/"

gh_pages_url = '' // path to GitHub repository in format git@github.com:{username}/{repo}.git
deploy = new GHPagesDeployer(site).deploy

// Custom commands-line commands.
commands = [
/*
 * Creates new page.
 *
 * location - relative path to the new page, should start with the /, i.e. /pages/index.html.
 * pageTitle - new page title
 */
'create-page': { String location, String pageTitle ->
        file = new File(content_dir, location)
        file.parentFile.mkdirs()
        file.exists() || file.write("""---
layout: site
title: "${pageTitle}"
published: true
---
""")},
/*
 * Creates new post.
 *
 * title - new post title
 */
'create-post': { String postTitle ->
            def date = new Date()
            def fileDate = date.format("yyyy-MM-dd")
            def filename = fileDate + "-" + postTitle.encodeAsSlug() + ".markdown"
            def blogDir = new File(content_dir + "${posts_base_url}")
            if (!blogDir.exists()) {
                blogDir.mkdirs()
            }
            def file = new File(blogDir, filename)

            file.exists() || file.write("""---
layout: post
title: "${postTitle}"
image:
date: "${date.format(datetime_format)}"
published: true
---
""")},

]