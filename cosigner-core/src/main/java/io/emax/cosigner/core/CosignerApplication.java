package io.emax.cosigner.core;

import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.setup.Environment;
import io.emax.cosigner.api.core.CurrencyPackageInterface;
import io.emax.cosigner.api.validation.Validator;
import io.emax.cosigner.core.cluster.ClusterInfo;
import io.emax.cosigner.core.cluster.Coordinator;
import io.emax.cosigner.core.resources.AdminResource;
import io.emax.cosigner.core.resources.CurrencyResource;
import io.emax.cosigner.core.resources.websocket.WebSocketSocket;
import io.emax.cosigner.validator.BasicValidator;

import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;

public class CosignerApplication extends io.dropwizard.Application<CosignerConfiguration> {
  private static CosignerConfiguration config;
  private static HashMap<String, CurrencyPackageInterface> currencies = new HashMap<>();
  private static LinkedList<Validator> validators = new LinkedList<>();
  private static final Logger LOGGER = LoggerFactory.getLogger(CosignerApplication.class);

  public static CosignerConfiguration getConfig() {
    return config;
  }

  public static void setConfig(CosignerConfiguration config) {
    CosignerApplication.config = config;
  }

  public static Map<String, CurrencyPackageInterface> getCurrencies() {
    return currencies;
  }

  public static List<Validator> getValidators() {
    return validators;
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

    // Load currency configurations packages defined in any of our libraries.
    String path = new File(
        CosignerApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            .getPath()).getParentFile().getPath();
    path += File.separator + "lib" + File.separator;

    File folder = new File(path);
    File[] listOfFiles = folder.listFiles();

    if (listOfFiles != null) {
      for (File listOfFile : listOfFiles) {
        if (listOfFile.isFile()) {

          JarFile jarFile = new JarFile(listOfFile.getAbsolutePath());
          Enumeration<JarEntry> e = jarFile.entries();

          URL[] urls = {new URL("jar:file:" + path + "!/")};
          URLClassLoader cl = URLClassLoader.newInstance(urls);

          while (e.hasMoreElements()) {
            JarEntry je = e.nextElement();
            if (je.isDirectory() || !je.getName().endsWith(".class")) {
              continue;
            }
            // -6 because of .class
            String className = je.getName().substring(0, je.getName().length() - 6);
            className = className.replace('/', '.');
            try {
              Class c = cl.loadClass(className);
              if (CurrencyPackageInterface.class.isAssignableFrom(c)) {
                CurrencyPackageInterface newCurrency = (CurrencyPackageInterface) c.newInstance();
                getCurrencies()
                    .put(newCurrency.getConfiguration().getCurrencySymbol(), newCurrency);
              }
            } catch (NoClassDefFoundError | Exception ex) {
              // It's ok if the class isn't valid, we may be trying to load the actual interface.
              LOGGER.debug("Failed to load", ex);
            }
          }
        }
      }
    }

    LOGGER.info("Currencies available for cosigner: " + getCurrencies().keySet());

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

    // Enable CORS
    final FilterRegistration.Dynamic cors =
        environment.servlets().addFilter("crossOriginRequests", CrossOriginFilter.class);
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

    // Trigger a status check on currencies.
    ClusterInfo.getInstance().updateCurrencyStatus();
  }
}
