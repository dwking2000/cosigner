package io.emax.cosigner.core;

import io.dropwizard.setup.Environment;
import io.emax.cosigner.core.cluster.ClusterInfo;
import io.emax.cosigner.core.cluster.Coordinator;
import io.emax.cosigner.core.currency.CurrencyPackage;
import io.emax.cosigner.core.resources.AdminResource;
import io.emax.cosigner.core.resources.CurrencyResource;

import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletRegistration;

public class CosignerApplication extends io.dropwizard.Application<CosignerConfiguration> {
  private static CosignerConfiguration config;
  private static HashMap<String, CurrencyPackage> currencies = new HashMap<>();
  private static Logger logger = LoggerFactory.getLogger(CosignerApplication.class);

  public static CosignerConfiguration getConfig() {
    return config;
  }

  public static void setConfig(CosignerConfiguration config) {
    CosignerApplication.config = config;
  }

  public static HashMap<String, CurrencyPackage> getCurrencies() {
    return currencies;
  }

  public static void setCurrencies(HashMap<String, CurrencyPackage> currencies) {
    CosignerApplication.currencies = currencies;
  }

  public static void main(String[] args) throws Exception {
    new CosignerApplication().run(args);
  }

  @Override
  public void run(CosignerConfiguration config, Environment environment) throws Exception {
    CosignerApplication.setConfig(config);

    // Initialize ClusterInfo
    ClusterInfo.getInstance();

    // Initialize the coordinator
    Coordinator.getInstance();

    // Load api.currency libraries here
    // Bitcoin
    CurrencyPackage bitcoinPackage = new CurrencyPackage();
    bitcoinPackage.setConfiguration(new io.emax.cosigner.bitcoin.BitcoinConfiguration());
    bitcoinPackage.setWallet(new io.emax.cosigner.bitcoin.BitcoinWallet());
    bitcoinPackage.setMonitor(new io.emax.cosigner.bitcoin.BitcoinMonitor());
    getCurrencies().put(bitcoinPackage.getConfiguration().getCurrencySymbol(), bitcoinPackage);
    // Ethereum
    CurrencyPackage ethereumPackage = new CurrencyPackage();
    ethereumPackage.setConfiguration(new io.emax.cosigner.ethereum.EthereumConfiguration());
    ethereumPackage.setWallet(new io.emax.cosigner.ethereum.EthereumWallet());
    ethereumPackage.setMonitor(new io.emax.cosigner.ethereum.EthereumMonitor());
    getCurrencies().put(ethereumPackage.getConfiguration().getCurrencySymbol(), ethereumPackage);

    // If the enabled currency list has been set, then remove any that aren't enabled.
    List<String> removeThese = new LinkedList<>();
    if (!config.getEnabledCurrencies().isEmpty()) {
      getCurrencies().forEach((symbol, currency) -> {
        if (!config.getEnabledCurrencies().contains(symbol)) {
          removeThese.add(symbol);
        }
      });
      removeThese.forEach(symbol -> {
        getCurrencies().remove(symbol);
      });
    }

    logger.info("Currencies enabled for cosigner: " + getCurrencies().keySet());

    // TODO Load any plugin libraries
    // TODO come up with a validation API maybe...
    // At the simplest level, validation would be a check on transaction rates for an address, if
    // too high in volume or amount, refuse to sign the transaction.

    // TODO Not likely done here, but create a node web app that acts as a simple interface to
    // cosigner.

    // Register WebSocket endpoints -- Everything after /ws/*
    AtmosphereServlet websocketServlet = new AtmosphereServlet();
    websocketServlet.framework().addInitParameter("com.sun.jersey.config.property.packages",
        "io.emax.cosigner.core.resources.WebSocketResource");
    websocketServlet.framework().addInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE,
        "application/json");
    websocketServlet.framework().addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true");
    ServletRegistration.Dynamic servletHolder =
        environment.servlets().addServlet("ws", websocketServlet);
    servletHolder.addMapping("/ws/*");

    // Register REST endpoints -- Everything with /rs/*
    final CurrencyResource currencyResource = new CurrencyResource();
    environment.jersey().register(currencyResource);

    // Register Admin endpoints (REST)
    final AdminResource adminResource = new AdminResource();
    environment.jersey().register(adminResource);
  }
}
