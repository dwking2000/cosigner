package io.emax.heimdal.API.REST;

import static io.emax.heimdal.common.Utility.exceptionToString;
import io.emax.heimdal.API.REST.Responses.ServerError;
import io.emax.heimdal.common.Ledger.Monitor;

import java.util.ArrayList;
import java.util.List;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.routing.Router;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class RestfulMonitor {
  final private Monitor monitor;

  public RestfulMonitor(Monitor monitor) {
    this.monitor = monitor;
  }

  /**
   * Path for getting the transactor's currency
   *
   * @param context The Restlet context
   * @return A restlet suitable for placing in a router or a component
   */
  private Restlet getCurrency(Context context) {
    return new Restlet(context) {
      @Override
      public void handle(final Request request, final Response response) {
        String input = "Default: Input was not read";
        try {
          response.setEntity(new Gson().toJson(monitor.getCurrency()), MediaType.APPLICATION_JSON);
          response.setStatus(Status.SUCCESS_OK);
        } catch (Exception e) {
          ServerError serverError = new ServerError(input, exceptionToString(e));
          response.setEntity(serverError.toString(), MediaType.TEXT_PLAIN);
          response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }
      }
    };
  }

  /**
   * Restlet Path for getting account balances from a monitor
   *
   * @param context The Restlet context
   * @return A restlet suitable for placing in a router or a component
   */
  private Restlet getBalances(Context context) {
    return new Restlet(context) {
      @Override
      public void handle(final Request request, final Response response) {
        Representation entity = request.getEntity();
        String input = "Default: Input was not read", output;
        List<String> accounts = null;
        try {
          if (entity.getMediaType() != null
              && entity.getMediaType().isCompatible(MediaType.APPLICATION_JSON)) {
            input = entity.getText();
            accounts = new Gson().fromJson(input, new TypeToken<ArrayList<String>>() {}.getType());
          }
          if (accounts == null || accounts.isEmpty())
            output = new Gson().toJson(monitor.getBalances());
          else
            output = new Gson().toJson(monitor.getBalances(accounts));
          response.setEntity(output, MediaType.APPLICATION_JSON);
          response.setStatus(Status.SUCCESS_OK);
        } catch (Exception e) {
          ServerError serverError = new ServerError(input, exceptionToString(e));
          response.setEntity(serverError.toString(), MediaType.TEXT_PLAIN);
          response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }
      }
    };
  }

  /**
   * Restlet Path for registering accounts to be monitored
   *
   * @param context The Restlet context
   * @return A restlet suitable for placing in a router or a component
   */
  private Restlet monitor(Context context) {
    return new Restlet(context) {
      @Override
      public void handle(final Request request, final Response response) {
        Representation entity = request.getEntity();
        String input = "Default: Input was not read";

        try {
          if (entity.getMediaType() != null
              && entity.getMediaType().isCompatible(MediaType.APPLICATION_JSON)) {
            input = entity.getText();
            List<String> accounts =
                new Gson().fromJson(input, new TypeToken<ArrayList<String>>() {}.getType());
            response.setEntity(new Gson().toJson(monitor.monitor(accounts)),
                MediaType.APPLICATION_JSON);
            response.setStatus(Status.SUCCESS_OK);
          } else {
            ServerError serverError =
                new ServerError(entity.getMediaType() == null ? input : entity.getText(),
                    "Could not parse user input to list of accounts to monitor");
            response.setEntity(serverError.toString(), MediaType.TEXT_PLAIN);
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
          }
        } catch (Exception e) {
          ServerError serverError = new ServerError(input, exceptionToString(e));
          response.setEntity(serverError.toString(), MediaType.TEXT_PLAIN);
          response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }
      }
    };
  }

  /**
   * Retrieve the Restlet Application
   *
   * @return A router Application with the restful endpoints "/getBalances", "/transfer",
   *         "/newAccount", and "/closeAccount"
   */
  public Application getRestletApplication() {
    return new Application() {
      public synchronized Restlet createInboundRoot() {
        // TODO: Create JSON Schema documentation for these endpoints
        return new Router(getContext()) {
          {
            attach("/getCurrency", getCurrency(getContext()));
            attach("/getBalances", getBalances(getContext()));
            attach("/monitor", getBalances(getContext()));
          }
        };
      }
    };
  }

}
