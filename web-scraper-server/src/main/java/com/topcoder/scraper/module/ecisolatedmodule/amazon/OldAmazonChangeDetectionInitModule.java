package com.topcoder.scraper.module.ecisolatedmodule.amazon;

import com.topcoder.common.config.AmazonProperty;
import com.topcoder.common.config.MonitorTargetDefinitionProperty;
import com.topcoder.common.dao.NormalDataDAO;
import com.topcoder.common.model.ProductInfo;
import com.topcoder.common.model.PurchaseHistory;
import com.topcoder.common.repository.NormalDataRepository;
import com.topcoder.common.traffic.TrafficWebClient;
import com.topcoder.scraper.Consts;
import com.topcoder.scraper.module.IChangeDetectionInitModule;
import com.topcoder.scraper.module.ecisolatedmodule.amazon.crawler.AmazonAuthenticationCrawler;
import com.topcoder.scraper.module.ecisolatedmodule.amazon.crawler.AmazonAuthenticationCrawlerResult;
import com.topcoder.scraper.module.ecisolatedmodule.amazon.crawler.AmazonPurchaseHistoryCrawler;
import com.topcoder.scraper.module.ecisolatedmodule.amazon.crawler.OldAmazonProductDetailCrawler;
import com.topcoder.scraper.module.ecisolatedmodule.crawler.AbstractProductCrawlerResult;
import com.topcoder.scraper.module.ecisolatedmodule.crawler.AbstractPurchaseHistoryCrawlerResult;
import com.topcoder.scraper.service.WebpageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Amazon implementation for ChangeDetectionInitModule
 */
public class OldAmazonChangeDetectionInitModule implements IChangeDetectionInitModule {

    private static Logger LOGGER = LoggerFactory.getLogger(OldAmazonChangeDetectionInitModule.class);

    private final AmazonProperty property;
    protected final MonitorTargetDefinitionProperty monitorTargetDefinitionProperty;
    protected final WebpageService webpageService;
    protected final NormalDataRepository repository;

    @Autowired
    public OldAmazonChangeDetectionInitModule(
            AmazonProperty property,
            MonitorTargetDefinitionProperty monitorTargetDefinitionProperty,
            WebpageService webpageService,
            NormalDataRepository repository
    ) {
        this.property = property;
        this.monitorTargetDefinitionProperty = monitorTargetDefinitionProperty;
        this.webpageService = webpageService;
        this.repository = repository;
    }

    @Override
    public String getModuleType() {
        return "amazon";
    }

    /**
     * Implementation of init method
     */
    @Override
    public void init(List<String> sites) throws IOException {
        for (MonitorTargetDefinitionProperty.MonitorTargetCheckSite monitorTargetCheckSite : monitorTargetDefinitionProperty.getCheckSites()) {
            if (!this.getModuleType().equalsIgnoreCase(monitorTargetCheckSite.getEcSite())) {
                continue;
            }
            for (MonitorTargetDefinitionProperty.MonitorTargetCheckPage monitorTargetCheckPage : monitorTargetCheckSite.getCheckPages()) {
                if (monitorTargetCheckPage.getPageName().equalsIgnoreCase(Consts.PURCHASE_HISTORY_LIST_PAGE_NAME)) {
                    List<String> usernameList = monitorTargetCheckPage.getCheckTargetKeys();

                    String passwordListString = System.getenv(Consts.AMAZON_CHECK_TARGET_KEYS_PASSWORDS);
                    if (passwordListString == null) {
                        LOGGER.error("Please set environment variable AMAZON_CHECK_TARGET_KEYS_PASSWORDS first");
                        throw new RuntimeException("environment variable AMAZON_CHECK_TARGET_KEYS_PASSWORDS not set");
                    }
                    List<String> passwordList = Arrays.asList(passwordListString.split(","));

                    for (int i = 0; i < usernameList.size(); i++) {
                        String username = usernameList.get(i);
                        String password = passwordList.get(i);

                        LOGGER.info("init ...");
                        TrafficWebClient webClient = new TrafficWebClient(0, false);
                        AmazonAuthenticationCrawler authenticationCrawler = new AmazonAuthenticationCrawler(property, webpageService);
                        AmazonAuthenticationCrawlerResult loginResult = authenticationCrawler.authenticate(webClient, username, password, null);
                        if (!loginResult.isSuccess()) {
                            LOGGER.error(String.format("Failed to login %s with username %s. Skip.", getModuleType(), username));
                            continue;
                        }

                        AmazonPurchaseHistoryCrawler purchaseHistoryCrawler = new AmazonPurchaseHistoryCrawler(webpageService);
                        AbstractPurchaseHistoryCrawlerResult crawlerResult = purchaseHistoryCrawler.fetchPurchaseHistoryList(webClient, null, false);
                        webClient.finishTraffic();
                        processPurchaseHistory(crawlerResult, username);
                    }

                } else if (monitorTargetCheckPage.getPageName().equalsIgnoreCase(Consts.PRODUCT_DETAIL_PAGE_NAME)) {
                    OldAmazonProductDetailCrawler crawler = new OldAmazonProductDetailCrawler(getModuleType(), property, webpageService);
                    for (String productCode : monitorTargetCheckPage.getCheckTargetKeys()) {
                        TrafficWebClient webClient = new TrafficWebClient(0, false);
                        AbstractProductCrawlerResult crawlerResult = crawler.fetchProductInfo(webClient, productCode, false);
                        webClient.finishTraffic();
                        processProductInfo(crawlerResult);
                    }

                } else {
                    throw new RuntimeException("Unknown monitor target definition " + monitorTargetCheckPage.getPageName());
                }

            }
        }
    }

    /**
     * Save normal data in database
     *
     * @param normalData normal data as string
     * @param page       the page name
     * @param pageKey    the page key
     */
    protected void saveNormalData(String normalData, String pageKey, String page) {
        NormalDataDAO dao = repository.findFirstByEcSiteAndPageAndPageKey(getModuleType(), page, pageKey);
        if (dao == null) {
            dao = new NormalDataDAO();
        }

        dao.setEcSite(getModuleType());
        dao.setNormalData(normalData);
        dao.setDownloadedAt(new Date());
        dao.setPage(page);
        dao.setPageKey(pageKey);
        repository.save(dao);
    }

    /**
     * process purchase history crawler result
     *
     * @param crawlerResult the crawler result
     * @param pageKey       the page key
     */
    protected void processPurchaseHistory(AbstractPurchaseHistoryCrawlerResult crawlerResult, String pageKey) {
        List<PurchaseHistory> purchaseHistoryList = crawlerResult.getPurchaseHistoryList();
        saveNormalData(PurchaseHistory.toArrayJson(purchaseHistoryList), pageKey, Consts.PURCHASE_HISTORY_LIST_PAGE_NAME);
    }

    /**
     * process product info crawler result
     *
     * @param crawlerResult the crawler result
     */
    protected void processProductInfo(AbstractProductCrawlerResult crawlerResult) {
        ProductInfo productInfo = crawlerResult.getProductInfo();
        saveNormalData(productInfo.toJson(), productInfo.getCode(), Consts.PRODUCT_DETAIL_PAGE_NAME);
    }
}
