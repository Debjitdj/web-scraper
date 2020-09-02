package com.topcoder.api.service;

import java.io.BufferedReader;
import java.io.*;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.topcoder.api.exception.ApiException;
import com.topcoder.api.exception.EntityNotFoundException;
import com.topcoder.common.dao.ConfigurationDAO;
import com.topcoder.common.repository.ConfigurationRepository;
import com.topcoder.common.repository.ECSiteAccountRepository;
import com.topcoder.scraper.module.ecunifiedmodule.dryrun.DryRunProductDetailModule;
import com.topcoder.scraper.module.ecunifiedmodule.dryrun.DryRunProductSearchModule;
import com.topcoder.scraper.module.ecunifiedmodule.dryrun.DryRunPurchaseHistoryModule;

/**
 * scraper service
 */
@Service
public class ConfigurationService {

  /**
   * the scraper Repository
   */
  @Autowired
  ConfigurationRepository configurationRepository;

  /**
   * ec site account repository
   */
  @Autowired
  ECSiteAccountRepository ecSiteAccountRepository;

  /**
   * dry run of PurchaseHistoryModule
   */
  @Autowired
  DryRunPurchaseHistoryModule dryRunPurchaseHistoryModule;

  /**
   * dry run of ProductModule
   */
  @Autowired
  DryRunProductDetailModule dryRunProductModule;

  /**
   * dry run of ProductSearchModule
   */
  @Autowired
  DryRunProductSearchModule dryRunProductSearchModule;

  /**
   * get config by site and type
   *
   * @param site the ec site
   * @param type the logic type
   * @return the config text
   * @throws EntityNotFoundException if not found
   */
  public String getConfig(String site, String type) throws EntityNotFoundException {
	ConfigurationDAO configurationDAO = get(site, type);
    if (configurationDAO == null) {
      throw new EntityNotFoundException("Cannot found config where site = " + site + " and " + type);
    }
	return get(site, type).getConfig();
  }

  /**
   * create or update ScraperDAO
   *
   * @param site the ec site
   * @param type the logic type
   * @param entity the request entity
   * @return the result message text
   * @throws ApiException if any error happened
   */
  public String createOrUpdateConfiguration(String site, String type, String conf) throws ApiException {
	try {
	  String resultText = "success ";

	  ConfigurationDAO configurationDAO = get(site, type);

	  if (configurationDAO == null) {
	    configurationDAO = new ConfigurationDAO();
	    resultText += "create record to scraper table";
	  } else {
	    resultText += "update record to scraper table";
	  }

      configurationDAO.setSite(site);
      configurationDAO.setType(type);
      configurationDAO.setConfig(conf);
      configurationRepository.save(configurationDAO);

      return resultText;

    } catch(Exception e) {
      e.printStackTrace();
	  throw new ApiException("failed to create or update conf");
    }
  }

  /**
   * execute conf
   *
   * @param site the ec site
   * @param type the logic type
   * @param request to executable conf
   * @throws ApiException if any error happened
   */
  public List<Object> executeConfiguration(String site, String type, String conf) throws ApiException {
    try {
      switch(type){
      case "purchase_history":
        return dryRunPurchaseHistoryModule.fetchPurchaseHistoryList(site, conf);
      case "product":
        return dryRunProductModule.fetchProductDetailList(site, conf);
      case "search":
        return dryRunProductSearchModule.searchProduct(site, conf);
      default:
        throw new ApiException("the type:" + type + " was not supported");
      }
    } catch(Exception e) {
      e.printStackTrace();
      throw new ApiException("failed to execute conf");
    }
  }

  /**
   *  get the html string
   *
   * @param the html file name
   * @return the html data
   * @throws ApiException if any error happened
   */
  public String getHtmlString(String htmlFileName) throws ApiException {
    try {
      String currentAbsolutePath = System.getProperty("user.dir");
      String htmlFilePath = searchHtmlFilePath(currentAbsolutePath + "/logs", htmlFileName);
      File htmlFile = new File(htmlFilePath);
//      FileReader fileReader = new FileReader(htmlFile, StandardCharsets.UTF_8);
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(htmlFile), "UTF-8"));
      StringBuffer htmlData = new StringBuffer();
      String tempHtmlData = "";
      while ((tempHtmlData = bufferedReader.readLine()) != null) {
        htmlData.append(tempHtmlData);
      }
      bufferedReader.close();
      return htmlData.toString();
    } catch(FileNotFoundException e) {
      e.printStackTrace();
      throw new ApiException(htmlFileName + " does not exist");
    } catch(Exception e) {
      e.printStackTrace();
      throw new ApiException("failed to get html file data");
    }
  }

  /**
   * search html file from path
   *
   * @param the directory path
   * @param the html file name
   * @return the html file path
   */
  private String searchHtmlFilePath(String directoryPath, String htmlFileName) {
    String htmlFilePath = "";
    File directory = new File(directoryPath);
    File files[] = directory.listFiles();
    for (int i = 0; i < files.length; i++) {
      String directoryOrFileName = files[i].getName();
      if (files[i].isDirectory()){
        htmlFilePath =  searchHtmlFilePath(directoryPath + "/" + directoryOrFileName, htmlFileName);
        if (!StringUtils.isEmpty(htmlFilePath)) {
          return htmlFilePath;
        }
      } else {
        if (directoryOrFileName.equals(htmlFileName + ".html")) {
          return directoryPath + "/" + directoryOrFileName;
        }
      }
    }
    return htmlFilePath;
  }

  /**
   * get ScraperDAO by site and type
   *
   * @param site the ec site
   * @param site the logic type
   * @return the ScraperDAO
   */
  public ConfigurationDAO get(String site, String type) {
	ConfigurationDAO configurationDAO = configurationRepository.findBySiteAndType(site, type);
    return configurationDAO;
  }

}
