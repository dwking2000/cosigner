package io.emax.heimdal.API.REST.Responses;

import com.google.gson.Gson;

public class ServerError {
  final public String error;
  final public String input;

  public ServerError(String input, String error) {
    this.error = error;
    this.input = input;
  }

  @Override
  public String toString() {
    return (new Gson()).toJson(this);
  }
}
