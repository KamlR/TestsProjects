package com.example.accountmanager;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.junit.jupiter.params.ParameterizedTest;

import static org.mockito.Mockito.*;

public class AccountManagerTest {
    @Mock
    IServer serverMock;
    IPasswordEncoder passwordEncoderMock;
    AccountManager accountManager;
    String login = "karina", password = "password";
    long sessionCode = 123L;
    double amount = 1000;
    String encodePassword;

    @BeforeEach
    void createObjects(){
        serverMock = Mockito.mock(IServer.class);
        passwordEncoderMock = Mockito.mock(IPasswordEncoder.class);
        accountManager = new AccountManager();
        accountManager.init(serverMock, passwordEncoderMock);
        workWithPassword();
        encodePassword = passwordEncoderMock.makeSecure(password);
    }

    @Test
    void testInitMethod(){
        // Проверим, что все значения корректно проинциализировались.
        accountManager.init(serverMock, passwordEncoderMock);
        Assertions.assertThrows(NullPointerException.class, () -> {
            accountManager.callLogin("karina", "12345");
        });
    }


    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void testCallLoginMethod(int step){
        // Хотим залогиниться, но такой юзер уже есть в бд (тут пока проверка ответа метода)
        if(step == 0){
            Mockito.doReturn(new ServerResponse(ServerResponse.ALREADY_LOGGED, sessionCode)).when(serverMock).login(login, encodePassword);
            Assertions.assertEquals(accountManager.callLogin(login, password).code, AccountManagerResponse.ACCOUNT_MANAGER_RESPONSE.code);
        }
        // Если пользователь ввёл некорректный пароль
        else if(step == 1){
            Mockito.doReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null)).when(serverMock).login(login, encodePassword);
            Assertions.assertEquals(accountManager.callLogin(login, password).code, AccountManagerResponse.NO_USER_INCORRECT_PASSWORD_RESPONSE.code);
        }
        // Если сервер вернёт success.
        else if(step == 2){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            AccountManagerResponse accountManagerResponse = accountManager.callLogin(login, password);
            Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.SUCCEED);
            Assertions.assertEquals(accountManagerResponse.response, sessionCode);
        }
        // Надо проверить, что при удачной авторизации юзера добавили в список авторизованных пользователей.
        else if(step == 3){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            // А теперь нужно проверить, что человек появился в списке авторизованных в данной сессии.
            Assertions.assertEquals(accountManager.callLogin(login, password).code, AccountManagerResponse.ACCOUNT_MANAGER_RESPONSE.code);
        }
        // Проверим, при авторизации уже залогиненного юзера его добавили в список авторизованных пользователей текущей сессии
        else if(step == 4){
            Mockito.doReturn(new ServerResponse(ServerResponse.ALREADY_LOGGED, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            AccountManagerResponse accountManagerResponse = accountManager.callLogin(login, password);
            Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.ACCOUNT_MANAGER_RESPONSE.code);

        }
        // В случае если сервер возвращает ошибку, которая не соотв интерфейсу методу (любой код, которого нет в списке)
        // Или если возвращает UNDEFINED_ERROR
        else if(step == 5){
            Mockito.doReturn(new ServerResponse(11, null)).when(serverMock).login(login, encodePassword);
            Assertions.assertEquals(accountManager.callLogin(login, password).code, AccountManagerResponse.INCORRECT_RESPONSE);

            Mockito.doReturn(new ServerResponse(ServerResponse.UNDEFINED_ERROR, null)).when(serverMock).login(login, encodePassword);
            Assertions.assertEquals(accountManager.callLogin(login, password).code, AccountManagerResponse.UNDEFINED_ERROR);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    void testCallLogout(int step){
        // Проверим, что человека в целом удалили из активной сесссии.
        if(step == 1){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(serverMock).logout(sessionCode);
            accountManager.callLogout(login, sessionCode);
            Assertions.assertEquals(accountManager.callLogout(login, sessionCode).code, AccountManagerResponse.NOT_LOGGED_RESPONSE.code);
        }
        // Если пользователь не авторизован или в целом его нет в бд.
        else if(step == 2){
            // Тут можно не мокать ответ сервера, так как если пользователь не авторизован, то мы туда не должны дойти
            Assertions.assertEquals(accountManager.callLogout(login, sessionCode).code, AccountManagerResponse.NOT_LOGGED_RESPONSE.code);

            // Если человека вообще нет в бд, но по какой-то причине он остался в списке текущей сессии.
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(ServerResponse.NOT_LOGGED, null)).when(serverMock).logout(sessionCode);
            Assertions.assertEquals(accountManager.callLogout(login, sessionCode).code, AccountManagerResponse.NOT_LOGGED_RESPONSE.code);
        }
        // Если сессия некорректна
        else if(step == 3){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Assertions.assertEquals(accountManager.callLogout(login, 545L).code, AccountManagerResponse.INCORRECT_SESSION);
        }
        // Если запрос проходит успешно
        else if(step == 4){
            // Залогинили юзера
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(serverMock).logout(sessionCode);
            Assertions.assertEquals(accountManager.callLogout(login, sessionCode).code, AccountManagerResponse.SUCCEED_RESPONSE.code);
        }
        // Если передана некорректная сессия, то юзера нельзя удалять из списка активных в сессии на текущий момент.
        else if(step == 5){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            accountManager.callLogout(login, 545L);
            Assertions.assertEquals(accountManager.callLogout(login, 545L).code, AccountManagerResponse.INCORRECT_SESSION);

        }
        // Проверим ответ, в случае если сервер вернул неизвестный код
        // Или если возвращает UNDEFINED_ERROR
        else if(step == 6){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(11, null)).when(serverMock).logout(sessionCode);
            Assertions.assertEquals(accountManager.callLogout(login, sessionCode).code, AccountManagerResponse.INCORRECT_RESPONSE);

            Mockito.doReturn(new ServerResponse(ServerResponse.UNDEFINED_ERROR, null)).when(serverMock).logout(sessionCode);
            Assertions.assertEquals(accountManager.callLogout(login, sessionCode).code, AccountManagerResponse.UNDEFINED_ERROR);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void withDrawTest(int step) {
        // Юзера нет в активной сессии авторизованных пользователей.
        // Юзер есть в активной сессии, но его нет в бд
        if (step == 1) {
            Assertions.assertEquals(accountManager.withdraw(login, sessionCode, amount).code, AccountManagerResponse.NOT_LOGGED_RESPONSE.code);

            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(ServerResponse.NOT_LOGGED, null)).when(serverMock).withdraw(sessionCode, amount);
            Assertions.assertEquals(accountManager.withdraw(login, sessionCode, amount).code, AccountManagerResponse.NOT_LOGGED_RESPONSE.code);
        }
        // Сессия не совпадает с переданной
        else if (step == 2) {
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Assertions.assertEquals(accountManager.withdraw(login, 545L, amount).code, AccountManagerResponse.INCORRECT_SESSION);
        }
        // Если нет денег на счету
        else if(step == 3){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(ServerResponse.NO_MONEY, amount-1000.0)).when(serverMock).withdraw(sessionCode, amount);
            // Нужно проверить, что код ответа верный и что в response вернули нужную сумму.
            AccountManagerResponse accountManagerResponse = accountManager.withdraw(login, sessionCode, amount);
            Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.NO_MONEY_RESPONSE.code);
            Assertions.assertEquals(accountManagerResponse.response, 0.0);
        }
        // Удачное снятие денег
        else if(step == 4){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, amount-200.0)).when(serverMock).withdraw(sessionCode, amount);
            // Проверим корректность кода ответа и что вернулось то число оставшихся денег, что мы замокали.
            AccountManagerResponse accountManagerResponse = accountManager.withdraw(login, sessionCode, amount);
            Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.SUCCEED);
            Assertions.assertEquals(accountManagerResponse.response, amount-200.0);
        }
        // Проверим ответ, в случае если сервер вернул неизвестный код
        // Или если возвращает UNDEFINED_ERROR
        else{
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(11, null)).when(serverMock).withdraw(sessionCode, amount);
            Assertions.assertEquals(accountManager.withdraw(login, sessionCode, amount).code, AccountManagerResponse.INCORRECT_RESPONSE);

            Mockito.doReturn(new ServerResponse(ServerResponse.UNDEFINED_ERROR, null)).when(serverMock).withdraw(sessionCode, amount);
            Assertions.assertEquals(accountManager.withdraw(login, sessionCode, amount).code, AccountManagerResponse.UNDEFINED_ERROR);
        }

    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void testDeposit(int step){
        // Если человек не авторизован.
        // Если авторизован, но нет в бд
        if(step == 1){
            Assertions.assertEquals(accountManager.deposit(login, sessionCode, amount).code, AccountManagerResponse.NOT_LOGGED_RESPONSE.code);

            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(ServerResponse.NOT_LOGGED, null)).when(serverMock).deposit(sessionCode, amount);
            Assertions.assertEquals(accountManager.deposit(login, sessionCode, amount).code, AccountManagerResponse.NOT_LOGGED_RESPONSE.code);
        }
        // Случай некорректной сессии
        else if(step == 2){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Assertions.assertEquals(accountManager.deposit(login, 545L, amount).code, AccountManagerResponse.INCORRECT_SESSION);
        }
        // Случай успешеного внесения средств на счёт
        else if(step == 3) {
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, amount)).when(serverMock).deposit(sessionCode, amount);
            AccountManagerResponse accountManagerResponse = accountManager.deposit(login, sessionCode, amount);
            Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.SUCCEED);
            Assertions.assertEquals(accountManagerResponse.response, amount);
        }
        // Проверим ответ, в случае если сервер вернул неизвестный код
        // Или если возвращает UNDEFINED_ERROR
        else{
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(11, null)).when(serverMock).deposit(sessionCode, amount);
            Assertions.assertEquals(accountManager.deposit(login, sessionCode, amount).code, AccountManagerResponse.INCORRECT_RESPONSE);

            Mockito.doReturn(new ServerResponse(ServerResponse.UNDEFINED_ERROR, null)).when(serverMock).deposit(sessionCode, amount);
            Assertions.assertEquals(accountManager.deposit(login, sessionCode, amount).code, AccountManagerResponse.UNDEFINED_ERROR);
        }

    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void testGetBalance(int step){
        // Если человек не авторизован.
        // Если авторизован, но нет в бд
        if(step == 1){
            Assertions.assertEquals(accountManager.getBalance(login, sessionCode).code, AccountManagerResponse.NOT_LOGGED_RESPONSE.code);

            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(ServerResponse.NOT_LOGGED, null)).when(serverMock).getBalance(sessionCode);
            Assertions.assertEquals(accountManager.getBalance(login, sessionCode).code, AccountManagerResponse.NOT_LOGGED_RESPONSE.code);
        }
        // Случай некорректной сессии
        else if(step == 2){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Assertions.assertEquals(accountManager.getBalance(login, 545L).code, AccountManagerResponse.INCORRECT_SESSION);
        }
        // Успешно получаем данные баланса
        else if(step == 3){
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, amount)).when(serverMock).getBalance(sessionCode);
            AccountManagerResponse accountManagerResponse = accountManager.getBalance(login, sessionCode);
            Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.SUCCEED);
            Assertions.assertEquals(accountManagerResponse.response, amount);
        }
        // Проверим ответ, в случае если сервер вернул неизвестный код
        // Или если возвращает UNDEFINED_ERROR
        else{
            Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
            accountManager.callLogin(login, password);
            Mockito.doReturn(new ServerResponse(11, null)).when(serverMock).getBalance(sessionCode);
            Assertions.assertEquals(accountManager.getBalance(login, sessionCode).code, AccountManagerResponse.INCORRECT_RESPONSE);

            Mockito.doReturn(new ServerResponse(ServerResponse.UNDEFINED_ERROR, null)).when(serverMock).getBalance(sessionCode);
            Assertions.assertEquals(accountManager.getBalance(login, sessionCode).code, AccountManagerResponse.UNDEFINED_ERROR);
        }
    }

    private void workWithPassword(){
        Mockito.when(passwordEncoderMock.makeSecure(anyString())).thenAnswer(invocation -> {
            String password = invocation.getArgument(0);
            StringBuilder reversed = new StringBuilder(password).reverse();
            return reversed.toString();
        });
    }

    // Тестирование 1 сценария
    @Test
    void testFirstCase(){
        Mockito.doReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null)).when(serverMock).login(login + "m", encodePassword);
        // 12 и m просто для визуальности некорректных данных.
        Assertions.assertEquals(accountManager.callLogin(login + "m", password).code, AccountManagerResponse.NO_USER_INCORRECT_PASSWORD_RESPONSE.code);
        String newEncoded = passwordEncoderMock.makeSecure(password + "12");
        Mockito.doReturn(new ServerResponse(ServerResponse.NO_USER_INCORRECT_PASSWORD, null)).when(serverMock).login(login, newEncoded);
        Assertions.assertEquals(accountManager.callLogin(login, password + "12").code, AccountManagerResponse.NO_USER_INCORRECT_PASSWORD_RESPONSE.code);

        // Успешная авторизация
        Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
        AccountManagerResponse accountManagerResponse = accountManager.callLogin(login, password);
        Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.SUCCEED);
        Assertions.assertEquals(accountManagerResponse.response, sessionCode);

        // Запрос баланса
        Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, amount)).when(serverMock).getBalance(sessionCode);
        accountManagerResponse = accountManager.getBalance(login, sessionCode);
        Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.SUCCEED);
        Assertions.assertEquals(accountManagerResponse.response, amount);

        // Вносим 100 единиц
        Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, amount)).when(serverMock).deposit(sessionCode, 100);
        accountManagerResponse = accountManager.deposit(login, sessionCode, 100);
        Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.SUCCEED);
        Assertions.assertEquals(accountManagerResponse.response, amount);
    }

    @Test
    void testSecondCase(){
        // Авторизовались
        Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, sessionCode)).when(serverMock).login(login, encodePassword);
        accountManager.callLogin(login, password);

        // Снимаем 50 единиц (неудачно)
        Mockito.doReturn(new ServerResponse(ServerResponse.NO_MONEY, 0.0)).when(serverMock).withdraw(sessionCode, 50);
        AccountManagerResponse accountManagerResponse = accountManager.withdraw(login, sessionCode, 50);
        Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.NO_MONEY_RESPONSE.code);
        Assertions.assertEquals(accountManagerResponse.response, 0.0);

        // Запрос баланса
        Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, amount)).when(serverMock).getBalance(sessionCode);
        accountManagerResponse = accountManager.getBalance(login, sessionCode);
        Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.SUCCEED);
        Assertions.assertEquals(accountManagerResponse.response, amount);

        // Вносим 100 единиц
        Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, amount)).when(serverMock).deposit(sessionCode, 100);
        accountManagerResponse = accountManager.deposit(login, sessionCode, 100);
        Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.SUCCEED);
        Assertions.assertEquals(accountManagerResponse.response, amount);

        // Снимаем 50 единиц (некорректная сессия)
        accountManagerResponse = accountManager.withdraw(login, 545L, 50);
        Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.INCORRECT_SESSION_RESPONSE.code);

        // Удачно снимаем 50 единиц
        Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, amount-50)).when(serverMock).withdraw(sessionCode, 50);
        accountManagerResponse = accountManager.withdraw(login, sessionCode, 50);
        Assertions.assertEquals(accountManagerResponse.code, AccountManagerResponse.SUCCEED);
        Assertions.assertEquals(accountManagerResponse.response, amount-50);

        // Выходим из системы
        Mockito.doReturn(new ServerResponse(ServerResponse.SUCCESS, null)).when(serverMock).logout(sessionCode);
        Assertions.assertEquals(accountManager.callLogout(login, sessionCode).code, AccountManagerResponse.SUCCEED_RESPONSE.code);
    }
}
