package io.emax.heimdal.API.REST;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.restlet.*;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.routing.Router;

import io.emax.heimdal.API.REST.Responses.ServerError;
import io.emax.heimdal.common.Transfer;
import io.emax.heimdal.common.Ledger.Transactor;

import java.util.ArrayList;
import java.util.List;

import static io.emax.heimdal.common.Utility.exceptionToString;


public class RestfulTransactor {
    final private Transactor transactor;

    public RestfulTransactor(Transactor transactor) {
        this.transactor = transactor;
    }

    /**
     * Get the transactor's balance
     *
     * @param context The Restlet context
     * @return A restlet suitable for placing in a router or a component
     */
    private Restlet getBalance(Context context) {
        return new Restlet(context) {
            @Override
            public void handle(final Request request, final Response response) {
                String input = "Default: Input was not read";
                try {
                    response.setEntity(new Gson().toJson(transactor.getBalance()), MediaType.APPLICATION_JSON);
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
     * Restlet Path for transferring balances in the transactor
     *
     * @param context The Restlet context
     * @return A restlet suitable for placing in a router or a component
     */
    private Restlet transfer(Context context) {
        return new Restlet(context) {
            @Override
            public void handle(final Request request, final Response response) {
                Representation entity = request.getEntity();
                String input = "Default: Input was not read";
                try {
                    if (entity.getMediaType() != null &&
                            entity.getMediaType().isCompatible(MediaType.APPLICATION_JSON)) {
                        input = entity.getText();
                        List<Transfer> transfers = new Gson().fromJson(input, new TypeToken<ArrayList<Transfer>>() {}.getType());
                        response.setEntity(new Gson().toJson(transactor.transfer(transfers)), MediaType.APPLICATION_JSON);
                        response.setStatus(Status.SUCCESS_OK);
                    } else {
                        ServerError serverError = new ServerError(
                                entity.getMediaType() == null ? input : entity.getText(),
                                "Could not parse user input to list of transfers");
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
                    response.setEntity(new Gson().toJson(transactor.getCurrency()), MediaType.APPLICATION_JSON);
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
     * Path for opening a new account with the transactor
     *
     * @param context The Restlet context
     * @return A restlet suitable for placing in a router or a component
     */
    private Restlet newAccount(Context context) {
        return new Restlet(context) {
            @Override
            public void handle(final Request request, final Response response) {
                String input = "Default: Input was not read";
                try {
                    response.setEntity(new Gson().toJson(transactor.newAccount()), MediaType.APPLICATION_JSON);
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
     * Path for opening a new account in the transactor
     *
     * @param context The Restlet context
     * @return A restlet suitable for placing in a router or a component
     */
    private Restlet closeAccount(Context context) {
        return new Restlet(context) {
            @Override
            public void handle(final Request request, final Response response) {
                Representation entity = request.getEntity();
                String input = "Default: Input was not read";
                try {
                    if (entity.getMediaType() != null &&
                            entity.getMediaType().isCompatible(MediaType.APPLICATION_JSON)) {
                        input = entity.getText();
                        // TODO: get rid of dumb class
                        JsonObject args = new JsonParser().parse(input).getAsJsonObject();
                        String account = args
                                .get("account")
                                .getAsString();
                        String destinationAccountForRemainingBalance = args
                                .get("destinationAccountForRemainingBalance")
                                .getAsString();
                        String output = new Gson().toJson(
                                transactor.closeAccount(
                                        account,
                                        destinationAccountForRemainingBalance));
                        response.setEntity(output, MediaType.APPLICATION_JSON);
                        response.setStatus(Status.SUCCESS_OK);
                    } else {
                        ServerError serverError = new ServerError(
                                entity.getMediaType() == null ? input : entity.getText(),
                                "Could not parse user input to list of transfers");
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
     * @return A router Application with the restful endpoints "/getBalances", "/transfer", "/newAccount", and "/closeAccount"
     */
    public Application getRestletApplication() {
        return new Application() {
            public synchronized Restlet createInboundRoot() {
                // TODO: Create JSON Schema documentation for these endpoints
                return new Router(getContext()) {
                    {
                        attach("/getCurrency", getCurrency(getContext()));
                        attach("/getBalance", getBalance(getContext()));
                        attach("/transfer", transfer(getContext()));
                        attach("/newAccount", newAccount(getContext()));
                        attach("/closeAccount", closeAccount(getContext()));
                    }
                };
            }
        };
    }

}
