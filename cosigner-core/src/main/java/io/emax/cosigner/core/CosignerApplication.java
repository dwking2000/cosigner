package io.emax.cosigner.core;

import java.util.HashMap;

import javax.servlet.ServletRegistration;

import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;

import io.dropwizard.setup.Environment;
import io.emax.cosigner.core.cluster.ClusterInfo;
import io.emax.cosigner.core.cluster.Coordinator;
import io.emax.cosigner.core.currency.CurrencyPackage;
import io.emax.cosigner.core.resources.AdminResource;
import io.emax.cosigner.core.resources.CurrencyResource;

public class CosignerApplication extends io.dropwizard.Application<CosignerConfiguration> {
  private static CosignerConfiguration config;
  private static HashMap<String, CurrencyPackage> currencies = new HashMap<>();

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

    // TODO - Make each currency a toggle in the configuration, disable those not wanted.
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

    // [FUTURE] Load any plugin libraries

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
