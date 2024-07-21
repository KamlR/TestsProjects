package com.example.accountmanager;

import java.util.concurrent.ConcurrentHashMap;

public class AccountManager {
  private IServer server;
  private ConcurrentHashMap<String, Long> activeAccounts = new ConcurrentHashMap<>();
  private IPasswordEncoder passEncoder;
  public void init(IServer s, IPasswordEncoder encoder) {
    server = s;
    passEncoder = encoder;
  }
  public AccountManagerResponse callLogin(String login, String password) {
    Long session = activeAccounts.get(login);
    if(session!=null)
      return AccountManagerResponse.ACCOUNT_MANAGER_RESPONSE;
    ServerResponse ret = server.login(login, passEncoder.makeSecure(password));
    switch(ret.code) {
      case ServerResponse.ALREADY_LOGGED:
        activeAccounts.put(login, (Long) ret.response);
        return AccountManagerResponse.ACCOUNT_MANAGER_RESPONSE;
      case ServerResponse.NO_USER_INCORRECT_PASSWORD:
        return AccountManagerResponse.NO_USER_INCORRECT_PASSWORD_RESPONSE;
      case ServerResponse.UNDEFINED_ERROR:
        return AccountManagerResponse.UNDEFINED_ERROR_RESPONSE;
      case ServerResponse.SUCCESS:{
        Object answ = ret.response;
        if(answ instanceof Long) {
          activeAccounts.put(login, (Long) answ);
          return new AccountManagerResponse(AccountManagerResponse.SUCCEED, answ);
        }
        break;   
      }
    }
    return new AccountManagerResponse(AccountManagerResponse.INCORRECT_RESPONSE, ret);
  }
  public AccountManagerResponse callLogout(String user, long session) {
    Long storedSession = activeAccounts.get(user);
    if(storedSession == null)
      return AccountManagerResponse.NOT_LOGGED_RESPONSE;
    if(storedSession!=session){
       return AccountManagerResponse.INCORRECT_SESSION_RESPONSE;
    }
    ServerResponse resp = server.logout(session);
    switch(resp.code){
      case ServerResponse.NOT_LOGGED:
        activeAccounts.remove(user);
        return AccountManagerResponse.NOT_LOGGED_RESPONSE;
      case ServerResponse.SUCCESS:
        activeAccounts.remove(user);
        return AccountManagerResponse.SUCCEED_RESPONSE;
      case ServerResponse.UNDEFINED_ERROR:
        return AccountManagerResponse.UNDEFINED_ERROR_RESPONSE;
    }
    return new AccountManagerResponse(AccountManagerResponse.INCORRECT_RESPONSE, resp);
  }
  public AccountManagerResponse withdraw(String login, long session, double amount){
    Long stored = activeAccounts.get(login);
    if(stored == null)
      return AccountManagerResponse.NOT_LOGGED_RESPONSE;
    if(stored!=session)
      return AccountManagerResponse.INCORRECT_SESSION_RESPONSE;
    ServerResponse resp = server.withdraw(session, amount);
    switch(resp.code){
      case ServerResponse.NOT_LOGGED:
        return AccountManagerResponse.NOT_LOGGED_RESPONSE;
      case ServerResponse.NO_MONEY:
        Object r = resp.response;
        if(r!=null && r instanceof Double)
          return new AccountManagerResponse(AccountManagerResponse.NO_MONEY, (Double)r);
        break;
      case ServerResponse.UNDEFINED_ERROR:
        return AccountManagerResponse.UNDEFINED_ERROR_RESPONSE;
      case ServerResponse.SUCCESS:
        r = resp.response;
        if(r!=null && r instanceof Double)
          return new AccountManagerResponse(AccountManagerResponse.SUCCEED, (Double)r);
        break;
    }
    return new AccountManagerResponse(AccountManagerResponse.INCORRECT_RESPONSE, resp);
  }  
  public AccountManagerResponse deposit(String login, long session, double amount){
    Long stored = activeAccounts.get(login);
    if(stored == null)
      return AccountManagerResponse.NOT_LOGGED_RESPONSE;
    if(stored!=session)
      return AccountManagerResponse.INCORRECT_SESSION_RESPONSE;
    ServerResponse resp = server.deposit(session, amount);
    switch(resp.code){
      case ServerResponse.NOT_LOGGED:
        return AccountManagerResponse.NOT_LOGGED_RESPONSE;
      case ServerResponse.NO_MONEY:
        Object r = resp.response;
        if(r!=null && r instanceof Double)
          return new AccountManagerResponse(AccountManagerResponse.NO_MONEY, (Double)r);
        break;
      case ServerResponse.SUCCESS:
        r = resp.response;
        if(r!=null && r instanceof Double)
          return new AccountManagerResponse(AccountManagerResponse.SUCCEED, (Double)r);
        break;
      case ServerResponse.UNDEFINED_ERROR:
        return AccountManagerResponse.UNDEFINED_ERROR_RESPONSE;
    }
    return new AccountManagerResponse(AccountManagerResponse.INCORRECT_RESPONSE, resp);
  }  
  public AccountManagerResponse getBalance(String login, long session){
    Long stored = activeAccounts.get(login);
    if(stored == null)
      return AccountManagerResponse.NOT_LOGGED_RESPONSE;
    if(stored!=session)
      return AccountManagerResponse.INCORRECT_SESSION_RESPONSE;
    ServerResponse resp = server.getBalance(session);
    switch(resp.code){
      case ServerResponse.NOT_LOGGED:
        return AccountManagerResponse.NOT_LOGGED_RESPONSE;
      case ServerResponse.SUCCESS:
        Object r = resp.response;
        if(r!=null && r instanceof Double)
          return new AccountManagerResponse(AccountManagerResponse.SUCCEED, (Double)r);
        break;
      case ServerResponse.UNDEFINED_ERROR:
        return AccountManagerResponse.UNDEFINED_ERROR_RESPONSE;
    }
    return new AccountManagerResponse(AccountManagerResponse.INCORRECT_RESPONSE, resp);
  }
}
