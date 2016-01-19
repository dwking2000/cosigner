package io.emax.cosigner.core;

import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.setup.Environment;
import io.emax.cosigner.api.core.CurrencyPackage;
import io.emax.cosigner.api.validation.Validator;
import io.emax.cosigner.core.cluster.ClusterInfo;
import io.emax.cosigner.core.cluster.Coordinator;
import io.emax.cosigner.core.resources.AdminResource;
import io.emax.cosigner.core.resources.CurrencyResource;
import io.emax.cosigner.core.resources.websocket.WebSocketSocket;
import io.emax.cosigner.validator.BasicValidator;

import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRegistration;

public class CosignerApplication extends io.dropwizard.Application<CosignerConfiguration> {
  private static CosignerConfiguration config;
  private static HashMap<String, CurrencyPackage> currencies = new HashMap<>();
  private static LinkedList<Validator> validators = new LinkedList<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(CosignerApplication.class);

  public static CosignerConfiguration getConfig() {
    return config;
  }

  public static void setConfig(CosignerConfiguration config) {
    CosignerApplication.config = config;
  }

  public static Map<String, CurrencyPackage> getCurrencies() {
    return currencies;
  }

  public static void setCurrencies(Map<String, CurrencyPackage> currencies) {
    CosignerApplication.currencies = new HashMap<>();
    CosignerApplication.currencies.putAll(currencies);
  }

  public static List<Validator> getValidators() {
    return validators;
  }

  public static void setValidators(List<Validator> validators) {
    CosignerApplication.validators = new LinkedList<>();
    CosignerApplication.validators.addAll(validators);
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
      removeThese.forEach(symbol -> getCurrencies().remove(symbol));
    }

    LOGGER.info("Currencies enabled for cosigner: " + getCurrencies().keySet());

    BasicValidator validator = new BasicValidator();
    validators.add(validator);

    // Register WebSocket endpoints -- Everything after /ws/*
    ServletRegistration.Dynamic servletHolder =
        environment.servlets().addServlet("ws", WebSocketSocket.class);
    servletHolder.addMapping("/ws/*");

    // Register REST endpoints -- Everything with /rs/*
    final CurrencyResource currencyResource = new CurrencyResource();
    environment.jersey().register(currencyResource);

    // Register Admin endpoints (REST)
    final AdminResource adminResource = new AdminResource();
    final DropwizardResourceConfig jerseyConfig =
        new DropwizardResourceConfig(environment.metrics());
    JerseyContainerHolder jerseyContainerHolder =
        new JerseyContainerHolder(new ServletContainer(jerseyConfig));

    jerseyConfig.register(adminResource);

    environment.admin().addServlet("admin resources", jerseyContainerHolder.getContainer())
        .addMapping("/admin/*");
  }
}
