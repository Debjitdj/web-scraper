package com.topcoder.scraper;

import com.topcoder.scraper.command.impl.ChangeDetectionCheckCommand;
import com.topcoder.scraper.command.impl.ChangeDetectionInitCommand;
import com.topcoder.scraper.command.impl.ProductDetailCommand;
import com.topcoder.scraper.command.impl.PurchaseHistoryListCommand;
import com.topcoder.scraper.command.impl.UserEncoderCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AppRunner is an implementation of {@link ApplicationRunner}
 */
@Component
public class AppRunner implements ApplicationRunner {

  private static Logger LOGGER = LoggerFactory.getLogger(AppRunner.class);

  private final PurchaseHistoryListCommand purchaseHistoryListCommand;
  private final ProductDetailCommand productDetailCommand;
  private final ChangeDetectionInitCommand changeDetectionInitCommand;
  private final ChangeDetectionCheckCommand changeDetectionCheckCommand;
  private final UserEncoderCommand userEncoderCommand;

  @Autowired
  public AppRunner(PurchaseHistoryListCommand purchaseHistoryListCommand, ProductDetailCommand productDetailCommand,
                   ChangeDetectionInitCommand changeDetectionInitCommand, ChangeDetectionCheckCommand changeDetectionCheckCommand,
                   UserEncoderCommand userEncoderCommand) {
    this.purchaseHistoryListCommand  = purchaseHistoryListCommand;
    this.productDetailCommand        = productDetailCommand;
    this.changeDetectionInitCommand  = changeDetectionInitCommand;
    this.changeDetectionCheckCommand = changeDetectionCheckCommand;
    this.userEncoderCommand          = userEncoderCommand;
  }

  /**
   * Run different command based on batch args
   *
   * @param args ApplicationArguments from input
   */
  @Override
  public void run(ApplicationArguments args) {

    List<String> batches = args.getOptionValues("batch");
    if (batches == null) {
      usage();
      return;
    }

    if (batches.contains("purchase_history")) {
      purchaseHistoryListCommand.run(args);
    } else if (batches.contains("product")) {
      productDetailCommand.run(args);
    } else if (batches.contains("change_detection_init")) {
      changeDetectionInitCommand.run(args);
    } else if (batches.contains("change_detection_check")) {
      changeDetectionCheckCommand.run(args);
    } else if (batches.contains("encrypt_user")) {
      userEncoderCommand.run(args);
    } else {
      usage();
    }
  }

  private void usage() {
    LOGGER.info("\njava -jar web-scraper.jar --batch=[purchase_history|product] --site=amazon\n    --batch is required.\n    --site is optional.");
  }

}