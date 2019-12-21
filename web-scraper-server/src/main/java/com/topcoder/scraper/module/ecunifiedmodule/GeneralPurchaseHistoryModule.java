package com.topcoder.scraper.module.ecunifiedmodule;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.topcoder.api.service.login.yahoo.YahooLoginHandler;
import com.topcoder.common.dao.ECSiteAccountDAO;
import com.topcoder.common.model.PurchaseHistory;
import com.topcoder.common.repository.ECSiteAccountRepository;
import com.topcoder.common.traffic.TrafficWebClient;
import com.topcoder.common.util.Common;
import com.topcoder.scraper.module.IPurchaseHistoryModule;
import com.topcoder.scraper.module.ecunifiedmodule.crawler.GeneralPurchaseHistoryCrawler;
import com.topcoder.scraper.module.ecunifiedmodule.crawler.GeneralPurchaseHistoryCrawlerResult;
import com.topcoder.scraper.service.PurchaseHistoryService;
import com.topcoder.scraper.service.WebpageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// TODO : implement, now this is just copyed from yahoo product detail
@Component
public class GeneralPurchaseHistoryModule implements IPurchaseHistoryModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeneralPurchaseHistoryModule.class);

  private final PurchaseHistoryService purchaseHistoryService;
  private final WebpageService webpageService;
  private final ECSiteAccountRepository ecSiteAccountRepository;
  private final YahooLoginHandler loginHandler;

  @Autowired
  public GeneralPurchaseHistoryModule(PurchaseHistoryService purchaseHistoryService,
                                      ECSiteAccountRepository ecSiteAccountRepository, WebpageService webpageService, YahooLoginHandler loginHandler) {
    this.purchaseHistoryService = purchaseHistoryService;
    this.webpageService = webpageService;
    this.ecSiteAccountRepository = ecSiteAccountRepository;
    this.loginHandler = loginHandler;
  }

  @Override
  public String getModuleType() {
    return "general";
  }

  @Override
  public void fetchPurchaseHistoryList(List<String> sites) throws IOException {
    // Iterable<ECSiteAccountDAO> accountDAOS =
    // ecSiteAccountRepository.findAllByEcSite(getModuleType());
    for (int i = 0; i < sites.size(); i++) {
      
      Iterable<ECSiteAccountDAO> accountDAOS = ecSiteAccountRepository.findAllByEcSite(sites.get(i));
      for (ECSiteAccountDAO ecSiteAccountDAO : accountDAOS) {

        if (ecSiteAccountDAO.getEcUseFlag() != Boolean.TRUE) {
          LOGGER.info("EC Site [" + ecSiteAccountDAO.getId() + ":" + ecSiteAccountDAO.getEcSite()
              + "] is not active. Skipped.");
          continue;
        }
        Optional<PurchaseHistory> lastPurchaseHistory = purchaseHistoryService.fetchLast(ecSiteAccountDAO.getId());

        TrafficWebClient webClient = new TrafficWebClient(ecSiteAccountDAO.getUserId(), true);
        LOGGER.info("web client version = " + webClient.getWebClient().getBrowserVersion());
        boolean restoreRet = Common.restoreCookies(webClient.getWebClient(), ecSiteAccountDAO);
        if (!restoreRet) {
          LOGGER.error("skip ecSite id = " + ecSiteAccountDAO.getId() + ", restore cookies failed");
          continue;
        }

        try {
          //KojimaPurchaseHistoryCrawler crawler = new KojimaPurchaseHistoryCrawler(getModuleType(), webpageService);
          GeneralPurchaseHistoryCrawler crawler = new GeneralPurchaseHistoryCrawler(sites.get(i), webpageService);


          GeneralPurchaseHistoryCrawlerResult crawlerResult = crawler.fetchPurchaseHistoryList(webClient,
              lastPurchaseHistory.orElse(null), true);
          webClient.finishTraffic();
          List<PurchaseHistory> list = crawlerResult.getPurchaseHistoryList();

          if (list != null && list.size() > 0) {
            //System.out.println("NULL PTR DEBUG. >>> before list.forEach command <<<");
            list.forEach(purchaseHistory -> purchaseHistory.setAccountId(Integer.toString(ecSiteAccountDAO.getId())));
           // System.out.println("NULL PTR DEBUG. getModuleType(): " + getModuleType());
            System.out.println("NULL PTR DEBUG. historylist: " + list);

            purchaseHistoryService.save(getModuleType(), list);
          }
          LOGGER.info("succeed fetch purchaseHistory for ecSite id = " + ecSiteAccountDAO.getId());
        } catch (Exception e) { // here catch all exception and did not throw it
          //this.loginHandler.saveFailedResult(ecSiteAccountDAO, e.getMessage());
          LOGGER.error("failed to PurchaseHistory for ecSite id = " + ecSiteAccountDAO.getId());
          e.printStackTrace();
        }
      }
    }
  }
}