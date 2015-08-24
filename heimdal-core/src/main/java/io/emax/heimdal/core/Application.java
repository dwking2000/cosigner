package io.emax.heimdal.core;

import java.util.HashMap;

import javax.servlet.ServletRegistration;

import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;

import io.dropwizard.setup.Environment;
import io.emax.heimdal.core.cluster.ClusterInfo;
import io.emax.heimdal.core.cluster.Coordinator;
import io.emax.heimdal.core.currency.CurrencyPackage;
import io.emax.heimdal.core.resources.AdminResource;
import io.emax.heimdal.core.resources.CurrencyResource;

public class Application extends io.dropwizard.Application<ApplicationConfiguration> {
  private static ApplicationConfiguration config;
  private static HashMap<String, CurrencyPackage> currencies = new HashMap<>();

  public static ApplicationConfiguration getConfig() {
    return config;
  }

  private static void setConfig(ApplicationConfiguration config) {
    Application.config = config;
  }

  public static HashMap<String, CurrencyPackage> getCurrencies() {
    return currencies;
  }

  public static void setCurrencies(HashMap<String, CurrencyPackage> currencies) {
    Application.currencies = currencies;
  }

  public static void main(String[] args) throws Exception {
    new Application().run(args);
  }

  @Override
  public void run(ApplicationConfiguration config, Environment environment) throws Exception {
    Application.setConfig(config);

    // Initialize ClusterInfo
    ClusterInfo.getInstance();

    // Initialize the coordinator
    Coordinator.getInstance();

    // Load api.currency libraries here
    // Bitcoin
    CurrencyPackage bitcoinPackage = new CurrencyPackage();
    bitcoinPackage.setConfiguration(new io.emax.heimdal.bitcoin.CurrencyConfiguration());
    bitcoinPackage.setWallet(new io.emax.heimdal.bitcoin.Wallet());
    bitcoinPackage.setMonitor(new io.emax.heimdal.bitcoin.Monitor());
    getCurrencies().put(bitcoinPackage.getConfiguration().getCurrencySymbol(), bitcoinPackage);
    // Ethereum
    CurrencyPackage ethereumPackage = new CurrencyPackage();
    ethereumPackage.setConfiguration(new io.emax.heimdal.ethereum.CurrencyConfiguration());
    ethereumPackage.setWallet(new io.emax.heimdal.ethereum.Wallet());
    ethereumPackage.setMonitor(new io.emax.heimdal.ethereum.Monitor());
    getCurrencies().put(ethereumPackage.getConfiguration().getCurrencySymbol(), ethereumPackage);

    // [FUTURE] Load any plugin libraries

    // Register WebSocket endpoints -- Everything after /ws/*
    AtmosphereServlet websocketServlet = new AtmosphereServlet();
    websocketServlet.framework().addInitParameter("com.sun.jersey.config.property.packages",
        "io.emax.heimdal.core.resources.WebSocketResource");
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
