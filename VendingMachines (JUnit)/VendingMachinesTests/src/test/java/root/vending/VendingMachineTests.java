package root.vending;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.xml.crypto.dsig.spec.HMACParameterSpec;
import java.util.ArrayList;

public class VendingMachineTests {
    VendingMachine machine;

    @BeforeEach
    void createObject(){
        machine = new VendingMachine();
    }

    @ParameterizedTest
    @MethodSource("generateDataForGetCurrentSum")
    void testGetCurrentSum(int expected){
        if(expected == 0){
            // Тут я тестирую, что при OPERATION вернётся 0
            // При этом тестирую не с 0 балансом, а докладываю денег
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(20, 30);
            machine.exitAdminMode();
            Assertions.assertEquals(expected, machine.getCurrentSum());
        }
        else if(expected == 80){
            // Тут я вначале меняю режим на ADMINISTERING и докладываю монеты
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(20, 30);
            Assertions.assertEquals(expected, machine.getCurrentSum());
        }
    }
    public static ArrayList<Integer>generateDataForGetCurrentSum(){
        ArrayList<Integer> expected = new ArrayList<>();
        // Если у нас режим OPERATION
        expected.add(0);
        // Если доложили 20 монет первого вида и 30 второго
        expected.add(80);
        return expected;
    }

    @ParameterizedTest
    @MethodSource("generateDataForGetCoins12")
    void testGetCoins1(int expected){
        if(expected == 0){
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(20, 30);
            machine.exitAdminMode();
            Assertions.assertEquals(expected, machine.getCoins1());
        }
        else{
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(20, 20);
            Assertions.assertEquals(expected, machine.getCoins1());
        }
    }

    @ParameterizedTest
    @MethodSource("generateDataForGetCoins12")
    void testGetCoins2(int expected){
        if(expected == 0){
            // Проверяем, что даже при доложенных деньгах, режим OPERATION вернёт 0
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(20, 30);
            machine.exitAdminMode();
            Assertions.assertEquals(expected, machine.getCoins2());
        }
        else{
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(20, 20);
            Assertions.assertEquals(expected, machine.getCoins2());
        }
    }
    public static ArrayList<Integer>generateDataForGetCoins12(){
        ArrayList<Integer> expected = new ArrayList<>();
        // Если был OPERATION
        expected.add(0);
        // Если режим поменяли и внесли деньги
        expected.add(20);
        return expected;
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void testFillProducts(int checkStep){
        // Проверяем работу метода в режиме OPERATION
        if (checkStep == 0){
            Assertions.assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, machine.fillProducts());
        }
        // Проверяем работу метода в режиме ADMINISTERING. Пока только на корректноет возвращаемое значение
        else if(checkStep == 1){
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(VendingMachine.Response.OK, machine.fillProducts());
        }
        // А тут проверим, что заполнение продуктами происходит корректно
        else{
            machine.enterAdminMode(117345294655382L);
            machine.fillProducts();
            Assertions.assertEquals(30, machine.getNumberOfProduct1());
            Assertions.assertEquals(40, machine.getNumberOfProduct2());
        }
    }


    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void testFillCoins(int checkStep){
        // Проверим, что не функционирует в режиме OPERATION
        if(checkStep == 0){
            Assertions.assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, machine.fillCoins(1, 1));
        }
        // Проверим, что в режиме администратора будет корректное возвращаемое значение
        else if(checkStep == 1){
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(VendingMachine.Response.OK, machine.fillCoins(1, 1));
        }
        // Проверим возвращаемое значение для метода, если c1 или c2 указать некорректными
        else if(checkStep == 2){
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.fillCoins(-4, 6));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.fillCoins(60, 6));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.fillCoins(6, -4));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.fillCoins(6, 60));
        }
        // Проверим, что количество монет корректно меняется
        else{
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(30, 20);
            Assertions.assertEquals(30, machine.getCoins1());
            Assertions.assertEquals(20, machine.getCoins2());

        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void testEnterAdminMode(int checkStep){
        // Проверю, что при несовпадении кода вернётся корректное сообщение
        if(checkStep == 0){
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.enterAdminMode(111));
        }
        // Проверим, что если есть деньги на счету, то получаем корректыный ответ
        else if(checkStep == 1){
            machine.putCoin1();
            Assertions.assertEquals(VendingMachine.Response.CANNOT_PERFORM, machine.enterAdminMode(117345294655382L));
        }
        // Теперь проверим, что при соблюдении всех требований автомат переходит в режим админа и возвращается OK
        else{
            Assertions.assertEquals(VendingMachine.Response.OK, machine.enterAdminMode(117345294655382L));
            // Так как кол-во продуктов можно только в режиме админа увеличить, проверим, что в него перешли
            Assertions.assertEquals(machine.fillProducts(), VendingMachine.Response.OK);
        }
    }

    @Test
    void testExitAdminMode() {
        // Для начала проверю, что если мы и так находимся в OPERATION, то всё ок
        machine.exitAdminMode();
        // Чтобы проверить, что мы и правда в OPERATION попробую звполнить автомат монетами
        Assertions.assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, machine.fillCoins(1, 1));

        // Теперь зайдём в режим администратора и попробуем выйти
        machine.enterAdminMode(117345294655382L);
        machine.exitAdminMode();
        Assertions.assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, machine.fillCoins(1, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void checkSetPrices(int checkStep){
        // Проверяем, что в режиме OPERATION работать не будет
        if(checkStep == 0){
            Assertions.assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, machine.setPrices(1, 1));
        }
        // Корректная проверка отрицательных цен
        else if(checkStep == 1){
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.setPrices(0, 1));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.setPrices(1, 0));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.setPrices(-1, 1));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.setPrices(1, -1));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.setPrices(0, 0));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.setPrices(-1, -1));
        }
        // И проверим, что цены и правды меняются и возвращется OK при корректных условиях
        else{
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(VendingMachine.Response.OK, machine.setPrices(250, 400));
            Assertions.assertEquals(250, machine.getPrice1());
            Assertions.assertEquals(400, machine.getPrice2());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void testPutCoin1(int checkStep){
        // Корректность режима
        if(checkStep == 0){
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, machine.putCoin1());
        }

        // Если нет места для coins1 в автомате
        else if(checkStep == 1){
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(50, 2);
            machine.exitAdminMode();
            Assertions.assertEquals(VendingMachine.Response.CANNOT_PERFORM, machine.putCoin1());
        }
        // Проверяем, что баланс и coins1 корректно увеличились
        else if(checkStep == 2){
            Assertions.assertEquals(VendingMachine.Response.OK, machine.putCoin1());
            Assertions.assertEquals(1, machine.getCurrentBalance());
        }
    }


    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void testPutCoin2(int checkStep){
        // Корректность режима
        if(checkStep == 0){
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, machine.putCoin2());
        }

        // Если нет места для coins1 в автомате
        else if(checkStep == 1){
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(2, 50);
            machine.exitAdminMode();
            Assertions.assertEquals(VendingMachine.Response.CANNOT_PERFORM, machine.putCoin2());
        }
        // Проверяем, что баланс и coins1 корректно увеличились
        else if(checkStep == 2){
            Assertions.assertEquals(VendingMachine.Response.OK, machine.putCoin2());
            Assertions.assertEquals(2, machine.getCurrentBalance());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void testReturnMoney(int checkStep){
        // Корректность режима
        if(checkStep == 0){
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, machine.returnMoney());
        }
        // Проверка на возврат 0 баланса
        else if(checkStep == 1){
            Assertions.assertEquals(VendingMachine.Response.OK, machine.returnMoney());
            Assertions.assertEquals(0, machine.getCurrentBalance());
        }
        // Проверка, когда отдаём все монеты второго вида
        else if(checkStep == 2){
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(12, 1);
            machine.exitAdminMode();
            machine.putCoin1();
            machine.putCoin1();
            machine.putCoin1();
            machine.putCoin2();
            machine.putCoin2();
            Assertions.assertEquals(VendingMachine.Response.OK, machine.returnMoney());
            Assertions.assertEquals(0, machine.getCurrentBalance());
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(0, machine.getCoins2());
            Assertions.assertEquals(14, machine.getCoins1());
        }
        else if(checkStep == 3){
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(1, 24);
            machine.exitAdminMode();
            for (int i = 0; i < 2; i++) {
                machine.putCoin2();
            }
            Assertions.assertEquals(VendingMachine.Response.OK, machine.returnMoney());
            Assertions.assertEquals(0, machine.getCurrentBalance());
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(24, machine.getCoins2());
            Assertions.assertEquals(1, machine.getCoins1());
        }
        else if(checkStep == 4){
            machine.enterAdminMode(117345294655382L);
            machine.fillCoins(1, 12);
            machine.exitAdminMode();
            machine.putCoin2();
            machine.putCoin2();
            machine.putCoin1();
            machine.returnMoney();
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(12, machine.getCoins2());
            Assertions.assertEquals(1, machine.getCoins1());

        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void testGiveProduct1(int checkSteps){
        if(checkSteps == 0){
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, machine.giveProduct1(12));

            machine.exitAdminMode();
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.giveProduct1(0));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.giveProduct1(-4));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.giveProduct1(31));
            Assertions.assertEquals(VendingMachine.Response.INSUFFICIENT_PRODUCT, machine.giveProduct1(2));
        }
        else if(checkSteps == 1){
            machine.enterAdminMode(117345294655382L);
            machine.fillProducts();
            machine.exitAdminMode();
            Assertions.assertEquals(VendingMachine.Response.INSUFFICIENT_MONEY, machine.giveProduct1(2));
        }
        // Когда сдача больше, чем кол-во монет 2 вида
        else if(checkSteps == 2){
            machine.enterAdminMode(117345294655382L);
            machine.fillProducts();
            machine.exitAdminMode();
            for (int i = 0; i < 2; i++) {
               machine.putCoin2();
            }
            for (int i = 0; i < 10; i++) {
                machine.putCoin1();
            }
            Assertions.assertEquals(VendingMachine.Response.OK, machine.giveProduct1(1));
            // Проверка баланса
            Assertions.assertEquals(0, machine.getCurrentBalance());
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(8, machine.getCoins1());
            Assertions.assertEquals(0, machine.getCoins2());
            Assertions.assertEquals(29, machine.getNumberOfProduct1());

        }

        // Когда сдача чётная
        else if(checkSteps == 3){ // 14 на счёту,  1 type - 11, 2 type - 26    23
            machine.enterAdminMode(117345294655382L);
            machine.fillProducts();
            machine.fillCoins(1, 24);
            machine.exitAdminMode();
            for (int i = 0; i < 2; i++) {
                machine.putCoin2();
            }
            for (int i = 0; i < 10; i++) {
                machine.putCoin1();
            }
            Assertions.assertEquals(VendingMachine.Response.OK, machine.giveProduct1(1));
            Assertions.assertEquals(0, machine.getCurrentBalance());
            Assertions.assertEquals(29, machine.getNumberOfProduct1());
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(23, machine.getCoins2());
            Assertions.assertEquals(11, machine.getCoins1());

        }
        else{ // 17 - 8 = 9
            machine.enterAdminMode(117345294655382L);
            machine.fillProducts();
            machine.fillCoins(1, 24);
            machine.exitAdminMode();
            for (int i = 0; i < 2; i++) {
                machine.putCoin2();
            }
            for (int i = 0; i < 13; i++) {
                machine.putCoin1();
            }

            Assertions.assertEquals(VendingMachine.Response.OK, machine.giveProduct1(1));
            Assertions.assertEquals(0, machine.getCurrentBalance());
            Assertions.assertEquals(29, machine.getNumberOfProduct1());
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(22, machine.getCoins2());
            Assertions.assertEquals(13, machine.getCoins1());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void testGiveProduct2(int checkSteps){
        if(checkSteps == 0){
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, machine.giveProduct2(12));
            machine.exitAdminMode();
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.giveProduct2(0));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.giveProduct2(-4));
            Assertions.assertEquals(VendingMachine.Response.INVALID_PARAM, machine.giveProduct2(41));
            Assertions.assertEquals(VendingMachine.Response.INSUFFICIENT_PRODUCT, machine.giveProduct2(2));
        }
        else if(checkSteps == 1){
            machine.enterAdminMode(117345294655382L);
            machine.fillProducts();
            machine.exitAdminMode();
            Assertions.assertEquals(VendingMachine.Response.INSUFFICIENT_MONEY, machine.giveProduct2(2));
        }
        // Когда сдача больше, чем кол-во монет 2 вида
        else if(checkSteps == 2){ // 14 - 5 = 9, 9 - 6
            machine.enterAdminMode(117345294655382L);
            machine.fillProducts();
            machine.fillCoins(1, 1);
            machine.exitAdminMode();
            for (int i = 0; i < 2; i++) {
                machine.putCoin2();
            }
            for (int i = 0; i < 10; i++) {
                machine.putCoin1();
            }
            Assertions.assertEquals(VendingMachine.Response.OK, machine.giveProduct2(1));
            // Проверка баланса
            Assertions.assertEquals(0, machine.getCurrentBalance());
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(8, machine.getCoins1());
            Assertions.assertEquals(0, machine.getCoins2());
            Assertions.assertEquals(39, machine.getNumberOfProduct2());

        }

        // Когда сдача чётная
        else if(checkSteps == 3){ // 15 на счету,  15 - 5 = 10
            machine.enterAdminMode(117345294655382L);
            machine.fillProducts();
            machine.fillCoins(1, 24);
            machine.exitAdminMode();
            for (int i = 0; i < 2; i++) {
                machine.putCoin2();
            }
            for (int i = 0; i < 11; i++) {
                machine.putCoin1();
            }
            Assertions.assertEquals(VendingMachine.Response.OK, machine.giveProduct2(1));
            Assertions.assertEquals(0, machine.getCurrentBalance());
            Assertions.assertEquals(39, machine.getNumberOfProduct2());
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(21, machine.getCoins2());
            Assertions.assertEquals(12, machine.getCoins1());

        }
        else{ // 14 - 5 = 9
            machine.enterAdminMode(117345294655382L);
            machine.fillProducts();
            machine.fillCoins(1, 24);
            machine.exitAdminMode();
            for (int i = 0; i < 2; i++) {
                machine.putCoin2();
            }
            for (int i = 0; i < 10; i++) {
                machine.putCoin1();
            }

            Assertions.assertEquals(VendingMachine.Response.OK, machine.giveProduct2(1));
            Assertions.assertEquals(0, machine.getCurrentBalance());
            Assertions.assertEquals(39, machine.getNumberOfProduct2());
            machine.enterAdminMode(117345294655382L);
            Assertions.assertEquals(22, machine.getCoins2());
            Assertions.assertEquals(10, machine.getCoins1());
        }

    }
}
