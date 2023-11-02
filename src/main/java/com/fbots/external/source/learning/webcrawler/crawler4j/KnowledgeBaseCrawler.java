package com.fbots.external.source.learning.webcrawler.crawler4j;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.http.Header;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class KnowledgeBaseCrawler extends edu.uci.ics.crawler4j.crawler.WebCrawler {
    private final AtomicInteger numSeenImages;

    static Map<String, String> uncrawledUrls = new HashMap<>();

    public KnowledgeBaseCrawler(AtomicInteger numSeenImages) {
        this.numSeenImages = numSeenImages;
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();

        return href.contains("https://help.zenoti.com/en/appointments");
    }

    /**
     * This function is called when a page is fetched and ready to be processed
     * by your program.
     */
    @Override
    public void visit(Page page) {
        int docid = page.getWebURL().getDocid();
        String url = page.getWebURL().getURL();
        String domain = page.getWebURL().getDomain();
        String path = page.getWebURL().getPath();
        String subDomain = page.getWebURL().getSubDomain();
        String parentUrl = page.getWebURL().getParentUrl();
        String anchor = page.getWebURL().getAnchor();

        logger.debug("Docid: {}", docid);
        logger.info("URL: {}", url);
        logger.debug("Domain: '{}'", domain);
        logger.debug("Sub-domain: '{}'", subDomain);
        logger.debug("Path: '{}'", path);
        logger.debug("Parent page: {}", parentUrl);
        logger.debug("Anchor text: {}", anchor);

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String text = htmlParseData.getText();
            String html = htmlParseData.getHtml();
            Document document = Jsoup.parse(html, page.getWebURL().getURL());
//            document.outputSettings(new Document.OutputSettings().prettyPrint(false));//makes html() preserve linebreaks and spacing
//            document.select("br").append("\\n");
//            document.select("p").prepend("\\n\\n");
//            String s = document.html().replaceAll("\\\\n", "\n");

            Set<WebURL> links = htmlParseData.getOutgoingUrls();
            String fileName = url.replaceAll("[\\/:*?\"<>|]", "_"); // Generate a safe filename
            saveHtmlToFile(html, fileName);

            List<String> tagsToRemove = Arrays.asList("header", "footer");
            tagsToRemove.forEach(tag -> {
                Elements elements = document.select(tag);
                elements.remove();
            });
            saveTextTofile(document.text(), fileName);

            //Boilerpipe
//            String htmlText;
//            try {
//                htmlText = LargestContentExtractor.INSTANCE.getText(html);
//                saveTextTofile(htmlText, fileName);
//            } catch (BoilerpipeProcessingException e) {
//                throw new RuntimeException(e);
//            }

            // HtmlCleaner
//            CleanerProperties props = new CleanerProperties();
//            props.setPruneTags("script");
//            String result = new HtmlCleaner(props).clean(html).getText().toString();
//            saveTextTofile(result, fileName);

            logger.debug("Text length: {}", text.length());
            logger.debug("Html length: {}", html.length());
            logger.debug("Number of outgoing links: {}", links.size());
        }

        Header[] responseHeaders = page.getFetchResponseHeaders();
        if (responseHeaders != null) {
            logger.debug("Response headers:");
            for (Header header : responseHeaders) {
                logger.debug("\t{}: {}", header.getName(), header.getValue());
            }
        }

        logger.debug("=============");
    }

    private void saveTextTofile(String text, String fileName) {
        fileName = fileName + ".txt";
        File file = new File("/Users/pravekumar/Crawler/", fileName);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(text);
            System.out.println("Saved txt content to: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save txt content to file: " + file.getAbsolutePath());
        }
    }

    private void saveHtmlToFile(String htmlContent, String fileName) {
        fileName = fileName + ".html";
        File file = new File("/Users/pravekumar/Crawler/", fileName);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(htmlContent);
            System.out.println("Saved HTML content to: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save HTML content to file: " + file.getAbsolutePath());
        }
    }

    @Override
    protected void onContentFetchError(Page page) {
        super.onContentFetchError(page);
        uncrawledUrls.put(page.getWebURL().getURL(), "Failed to fetch content");
    }

    @Override
    protected void onPageBiggerThanMaxSize(String urlStr, long pageSize) {
        super.onPageBiggerThanMaxSize(urlStr, pageSize);
        uncrawledUrls.put(urlStr, "Page Size bigger than max size");
    }

    @Override
    protected void onParseError(WebURL webUrl) {
        super.onParseError(webUrl);
        uncrawledUrls.put(webUrl.getURL(), "Failed to parse url");
    }

    @Override
    protected void onUnexpectedStatusCode(String urlStr, int statusCode, String contentType, String description) {
        super.onUnexpectedStatusCode(urlStr, statusCode, contentType, description);
        uncrawledUrls.put(urlStr, "Got unexpected error code while crawling: " + statusCode + " description: " + description);
    }

    @Override
    protected void onUnhandledException(WebURL webUrl, Throwable e) {
        super.onUnhandledException(webUrl, e);
        uncrawledUrls.put(webUrl.getURL(), "Got exception while crawling: " + e);
    }

    @Override
    protected void onRedirectedStatusCode(Page page) {
        super.onRedirectedStatusCode(page);
        uncrawledUrls.put(page.getWebURL().getURL(), "Redirected ");
    }

    @Override
    protected void handlePageStatusCode(WebURL webUrl, int statusCode, String statusDescription) {
        super.handlePageStatusCode(webUrl, statusCode, statusDescription);
        //uncrawledUrls.put(webUrl.getURL(), "Page status code: " + statusCode + " status Description: " + statusDescription);
    }

    public static void main(String[] args) throws Exception {
        CrawlConfig config = new CrawlConfig();

        config.setCrawlStorageFolder("/Users/pravekumar/Crawler/");

        config.setPolitenessDelay(1000);

        config.setMaxDepthOfCrawling(10);

        config.setMaxPagesToFetch(500);

        config.setUserAgentString("test");

        // Should binary data should also be crawled? example: the contents of pdf, or the metadata of images etc
        //config.setIncludeBinaryContentInCrawling(false);

        // Do you need to set a proxy? If so, you can use:
        // config.setProxyHost("proxyserver.example.com");
        // config.setProxyPort(8080);

        // If your proxy also needs authentication:
        // config.setProxyUsername(username); config.getProxyPassword(password);

        config.setResumableCrawling(false);

        // Instantiate the controller for this crawl.
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        // For each crawl, you need to add some seed urls. These are the first
        // URLs that are fetched and then the crawler starts following links
        // which are found in these pages
        controller.addSeed("https://help.zenoti.com/en/appointments.html");

        int numberOfCrawlers = 10;

        // To demonstrate an example of how you can pass objects to crawlers, we use an AtomicInteger that crawlers
        // increment whenever they see a url which points to an image.
        AtomicInteger numSeenImages = new AtomicInteger();

        // The factory which creates instances of crawlers.
        CrawlController.WebCrawlerFactory<KnowledgeBaseCrawler> factory = () -> new KnowledgeBaseCrawler(numSeenImages);

        // Start the crawl. This is a blocking operation, meaning that your code
        // will reach the line after this only when crawling is finished.
        controller.start(factory, numberOfCrawlers);

        System.out.println("Url Info : " +  uncrawledUrls);

    }
}
