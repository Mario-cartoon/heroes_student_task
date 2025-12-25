package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.PrintBattleLog;
import com.battle.heroes.army.programs.SimulateBattle;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Этот метод осуществляет симуляцию боя между армией игрока и армией
 * компьютера.
 * Цель метода — провести бой, следуя установленным правилам.
 * Симуляция происходит следующим образом:
 * 
 * На каждом раунде юниты сортируются по убыванию значения атаки, чтобы первыми
 * ходили самые сильные.
 * Пока в обеих армиях есть живые юниты, они атакуют друг друга по очереди.
 * Если у одной из армий заканчиваются юниты, она ожидает завершения ходов
 * оставшихся юнитов противника.
 * Когда все юниты походили, раунд завершается, и начинается следующий.
 */
public class SimulateBattleImpl implements SimulateBattle {

    private PrintBattleLog printBattleLog;
    private final int roundDelay = 50; // задержка между раундами в миллисекундах

    /**
     * Устанавливает обработчик для логирования боевых действий.
     *
     * @param printBattleLog обработчик логов
     */
    public void setPrintBattleLog(PrintBattleLog printBattleLog) {
        this.printBattleLog = printBattleLog;
    }

    /**
     * Запускает симуляцию боя с чередованием первого хода:
     * нечётные раунды — игрок, чётные — компьютер.
     *
     * @param playerArmy   армия игрока
     * @param computerArmy армия компьютера
     * @throws InterruptedException если поток был прерван во время задержки
     */
    @Override
    public void simulate(Army playerArmy, Army computerArmy) throws InterruptedException {
        List<Unit> playerUnits = getAliveUnits(playerArmy);
        List<Unit> computerUnits = getAliveUnits(computerArmy);

        int round = 1;

        while (!playerUnits.isEmpty() && !computerUnits.isEmpty()) {
            // Обновляем списки живых юнитов и сортируем по убыванию атаки
            playerUnits = getSortedAliveUnits(playerUnits);
            computerUnits = getSortedAliveUnits(computerUnits);

            boolean playerStartsFirst = (round % 2 == 1);
            executeAlternatingMoves(playerUnits, computerUnits, playerStartsFirst);

            round++;
            Thread.sleep(roundDelay);
        }

        determineWinner(playerUnits, computerUnits);
    }

    /**
     * Выполняет поочерёдные ходы
     *
     * @param playerUnits      живые юниты игрока
     * @param computerUnits    живые юниты компьютера
     * @param playerMovesFirst true — игрок начинает, false — компьютер
     */
    private void executeAlternatingMoves(List<Unit> playerUnits, List<Unit> computerUnits, boolean playerMovesFirst) {
        int playerIndex = 0;
        int computerIndex = 0;
        boolean playerTurn = playerMovesFirst;

        int maxMoves = Math.max(playerUnits.size(), computerUnits.size()) * 2;

        for (int move = 0; move < maxMoves; move++) {
            if (playerTurn) {
                if (playerIndex < playerUnits.size()) {
                    Unit attacker = playerUnits.get(playerIndex);
                    if (attacker.isAlive()) {
                        executeSingleAttack(attacker, computerUnits);
                        playerIndex++;
                    } else {
                        playerIndex++; // пропускаем мёртвого
                        move--; // не считаем за ход
                    }
                }
            } else {
                if (computerIndex < computerUnits.size()) {
                    Unit attacker = computerUnits.get(computerIndex);
                    if (attacker.isAlive()) {
                        executeSingleAttack(attacker, playerUnits);
                        computerIndex++;
                    } else {
                        computerIndex++;
                        move--;
                    }
                }
            }

            playerTurn = !playerTurn;

            // Проверка окончания боя
            if (getAliveUnits(playerUnits).isEmpty() || getAliveUnits(computerUnits).isEmpty()) {
                break;
            }
        }
    }

    /**
     * Выполняет одну атаку от заданного юнита.
     *
     * @param attacker   атакующий юнит
     * @param enemyUnits список целей
     */
    private void executeSingleAttack(Unit attacker, List<Unit> enemyUnits) {
        try {
            Unit target = attacker.getProgram().attack();

            if (target != null && target.isAlive()) {
                if (printBattleLog != null) {
                    printBattleLog.printBattleLog(attacker, target);
                }

                applyDamage(attacker, target);

                if (!target.isAlive() && printBattleLog != null) {
                    printBattleLog.printBattleLog(target, attacker);
                }
            }
        } catch (Exception e) {
            // Ошибки при атаке не прерывают бой
        }
    }

    /**
     * Наносит урон цели на основе атаки и бонусов.
     *
     * @param attacker атакующий юнит
     * @param target   цель
     */
    private void applyDamage(Unit attacker, Unit target) {
        int damage = calculateEffectiveDamage(attacker, target);
        int newHealth = Math.max(0, target.getHealth() - damage);

        target.setHealth(newHealth);
        target.setAlive(newHealth > 0);
    }

    /**
     * Рассчитывает итоговый урон с учётом бонусов.
     *
     * @param attacker атакующий юнит
     * @param target   цель
     * @return наносимый урон (минимум 1)
     */
    private int calculateEffectiveDamage(Unit attacker, Unit target) {
        int baseDamage = attacker.getBaseAttack();

        if (hasCombatAdvantage(attacker, target)) {
            baseDamage = (int) (baseDamage * 1.5);
        }

        return Math.max(1, baseDamage);
    }

    /**
     * Проверяет наличие преимущества.
     *
     * @param attacker атакующий юнит
     * @param target   цель
     * @return true, если есть преимущество
     */
    private boolean hasCombatAdvantage(Unit attacker, Unit target) {
        String attackerType = attacker.getUnitType();
        String targetType = target.getUnitType();

        return (attackerType.contains("Лучник") && targetType.contains("Мечник")) ||
                (attackerType.contains("Всадник") && targetType.contains("Лучник"));
    }

    /**
     * Возвращает отсортированный список живых юнитов по убыванию атаки.
     *
     * @param units исходный список юнитов
     * @return отсортированный список живых юнитов
     */
    private List<Unit> getSortedAliveUnits(List<Unit> units) {
        return units.stream()
                .filter(Unit::isAlive)
                .sorted((u1, u2) -> Integer.compare(u2.getBaseAttack(), u1.getBaseAttack()))
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список живых юнитов из армии.
     *
     * @param army армия
     * @return список живых юнитов
     */
    private List<Unit> getAliveUnits(Army army) {
        if (army == null || army.getUnits() == null) {
            return Collections.emptyList();
        }
        return getAliveUnits(army.getUnits());
    }

    /**
     * Возвращает список живых юнитов из заданного списка.
     *
     * @param units список юнитов
     * @return список живых юнитов
     */
    private List<Unit> getAliveUnits(List<Unit> units) {
        if (units == null) {
            return Collections.emptyList();
        }
        return units.stream()
                .filter(Unit::isAlive)
                .collect(Collectors.toList());
    }

    /**
     * Определяет и выводит победителя боя.
     *
     * @param playerUnits   оставшиеся юниты игрока
     * @param computerUnits оставшиеся юниты компьютера
     */
    private void determineWinner(List<Unit> playerUnits, List<Unit> computerUnits) {
        System.out.println("\n=== РЕЗУЛЬТАТ БОЯ ===");

        if (!playerUnits.isEmpty() && !computerUnits.isEmpty()) {
            System.out.println("НИЧЬЯ!");
        } else if (!playerUnits.isEmpty()) {
            System.out.println("ПОБЕДИЛ ИГРОК! Осталось юнитов: " + playerUnits.size());
        } else if (!computerUnits.isEmpty()) {
            System.out.println("ПОБЕДИЛ КОМПЬЮТЕР! Осталось юнитов: " + computerUnits.size());
        } else {
            System.out.println("ОБЕ АРМИИ УНИЧТОЖЕНЫ!");
        }
    }

    /**
     * Запускает симуляцию с заданной стратегией очередности ходов.
     *
     * @param playerArmy   армия игрока
     * @param computerArmy армия компьютера
     * @param strategy     стратегия: ALTERNATING, PLAYER_FIRST, COMPUTER_FIRST
     * @throws InterruptedException если поток был прерван
     */
    public void simulateWithStrategy(Army playerArmy, Army computerArmy, String strategy)
            throws InterruptedException {
        switch (strategy) {
            case "PLAYER_FIRST":
                simulateFixedOrder(playerArmy, computerArmy, true);
                break;
            case "COMPUTER_FIRST":
                simulateFixedOrder(playerArmy, computerArmy, false);
                break;
            case "ALTERNATING":
            default:
                simulate(playerArmy, computerArmy);
        }
    }

    /**
     * Симуляция с фиксированной очередностью: сначала все юниты одной стороны,
     * потом — другой.
     *
     * @param playerArmy   армия игрока
     * @param computerArmy армия компьютера
     * @param playerFirst  true — игрок ходит первым
     * @throws InterruptedException если поток был прерван
     */
    private void simulateFixedOrder(Army playerArmy, Army computerArmy, boolean playerFirst)
            throws InterruptedException {
        List<Unit> playerUnits = getAliveUnits(playerArmy);
        List<Unit> computerUnits = getAliveUnits(computerArmy);

        int round = 1;

        while (!playerUnits.isEmpty() && !computerUnits.isEmpty()) {
            if (playerFirst) {
                executeRoundFixedOrder(playerUnits, computerUnits);
            } else {
                executeRoundFixedOrder(computerUnits, playerUnits);
                // Меняем ссылки для корректной проверки в конце раунда
                List<Unit> temp = playerUnits;
                playerUnits = computerUnits;
                computerUnits = temp;
            }

            playerUnits = getAliveUnits(playerUnits);
            computerUnits = getAliveUnits(computerUnits);

            round++;
            Thread.sleep(roundDelay);
        }

        determineWinner(playerUnits, computerUnits);
    }

    /**
     * Выполняет фазу атаки для одной стороны, затем для другой.
     *
     * @param attackers атакующие юниты
     * @param defenders защищающиеся юниты
     */
    private void executeRoundFixedOrder(List<Unit> attackers, List<Unit> defenders) {
        for (Unit attacker : attackers) {
            if (attacker.isAlive()) {
                executeSingleAttack(attacker, defenders);
                if (getAliveUnits(defenders).isEmpty()) {
                    break;
                }
            }
        }
    }
}