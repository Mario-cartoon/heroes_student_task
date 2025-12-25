package programs;

import com.battle.heroes.army.Army;
import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.GeneratePreset;

import java.util.*;

/*
    GeneratePreset содержит в себе метод Army generate(List<Unit> unitList, int maxPoints), 
    который отвечает за генерацию пресета армии противника.
 */
public class GeneratePresetImpl implements GeneratePreset {
    // Максимальное кол-во юнитов одного типа
    public final static int MAX_COUNT_UNITS_PER_TYPE = 11;

    // Поле для хранения характеристик юнита
    public enum UnitField {
        COSTS, // Стоимость
        BASE_ATTACK, // Базовая атака
        HEALTHS // Здоровье
    }

    @Override
    public Army generate(List<Unit> unitList, int maxPoints) {
        Army army = new Army();

        if (unitList == null || unitList.isEmpty())
            return army;

        // Находим идеальный баланс {atack, health, countsUnits per types} с помощью
        // димнамического
        // программированиядля этого используем вспомогательный класс ArmyState
        int[] optimalCounts = calculateOptimalArmy(unitList, maxPoints);

        // Генирируем координаты и возвращаем
        return generateArmyCoordinates(unitList, optimalCounts, army);
    }

    /**
     * Алгоритм динамического программирования для оптимизации состава армии
     * Для этого создаем массив вариаций армий с лучшими очками
     * Потом выбираем лучшую вариацию
     * 
     * @param unitList  - список юнитов
     * @param maxPoints - максимальное количество очков 1500
     * @return массив с количеством юнитов по типам
     */
    private int[] calculateOptimalArmy(List<Unit> unitList, int maxPoints) {
        // Подсчитываем количество юнитов всего 4
        int typeCount = unitList.size();

        // Извлекаем характеристики для того чтобы было удобнее их извлекать в
        // последубщем
        Map<UnitField, int[]> unitData = extractUnitData(unitList, typeCount);
        int[] costs = unitData.get(UnitField.COSTS);
        int[] baseAttack = unitData.get(UnitField.BASE_ATTACK);
        int[] healths = unitData.get(UnitField.HEALTHS);

        // Суть создает массив вариаций армий с лучшими очками и кол-во юнитов
        ArmyState[] armyStateList = new ArmyState[maxPoints + 1];
        armyStateList[0] = new ArmyState(typeCount);

        // Обрабатываем каждый тип последовательно ятобы избежать повторений
        for (int type = 0; type < typeCount; type++) {

            // Идем от обратного чтобы отсечь ненужные варианты
            for (int points = maxPoints; points >= 0; points--) {
                if (armyStateList[points] == null)
                    continue;

                // Пробуем добавить от 1 до 11 юнитов этого типа
                for (int count = 1; count <= MAX_COUNT_UNITS_PER_TYPE; count++) {
                    int newCost = points + count * costs[type];
                    if (newCost > maxPoints)
                        break;

                    // Проверяем лимит на макс кол-во очков на один тип юнита
                    if (armyStateList[points].counts[type] + count > MAX_COUNT_UNITS_PER_TYPE) {
                        continue;
                    }

                    long newAttack = armyStateList[points].attack + count * (long) baseAttack[type];
                    long newHealth = armyStateList[points].health + count * (long) healths[type];

                    int[] newCounts = armyStateList[points].counts.clone();
                    newCounts[type] += count;
                    // Добавляем новый набор армии суть в том чтобы составить разные
                    // балансы по атаке и здоровью и после составления всх вариантов выбрать лучший
                    ArmyState newBestComboArmy = new ArmyState(newAttack, newHealth, newCounts);

                    if (armyStateList[newCost] == null || newBestComboArmy.isBetterThan(armyStateList[newCost])) {
                        armyStateList[newCost] = newBestComboArmy;
                    }
                }
            }
        }

        // Теперь из лучшей выборки находим лучшеий сет по атаке ,если атака равна то по
        // звдороью + количество юнитов по типу
        ArmyState bestState = null;
        for (int points = 0; points <= maxPoints; points++) {
            if (armyStateList[points] != null && (bestState == null || armyStateList[points].isBetterThan(bestState))) {
                bestState = armyStateList[points];
            }
        }
        return bestState != null ? bestState.counts : new int[typeCount];
    }

    private Map<UnitField, int[]> extractUnitData(List<Unit> unitList, int typeCount) {
        Map<UnitField, int[]> data = new EnumMap<>(UnitField.class);

        data.put(UnitField.COSTS, new int[typeCount]);
        data.put(UnitField.BASE_ATTACK, new int[typeCount]);
        data.put(UnitField.HEALTHS, new int[typeCount]);

        for (int i = 0; i < typeCount; i++) {
            Unit unit = unitList.get(i);
            data.get(UnitField.COSTS)[i] = unit.getCost();
            data.get(UnitField.BASE_ATTACK)[i] = unit.getBaseAttack();
            data.get(UnitField.HEALTHS)[i] = unit.getHealth();
        }

        return data;
    }

    private Army generateArmyCoordinates(List<Unit> unitList, int[] counts, Army army) {
        Random random = new Random();
        Set<String> usedCoordinates = new HashSet<>();
        int unitCounter = 0;

        // Определяем зону для армии компьютера
        final int MIN_X = 0; // Левая часть поля
        final int MAX_X = 2; // 3 колонки: 0, 1, 2
        final int MAX_Y = 20; // 21 строка: 0-20

        // Создаем юниты каждого типа
        for (int typeIndex = 0; typeIndex < unitList.size(); typeIndex++) {
            Unit template = unitList.get(typeIndex);
            int count = counts[typeIndex];

            if (count == 0)
                continue;

            for (int i = 0; i < count; i++) {
                // Генерируем уникальные координаты
                int x, y;
                String coordKey;
                int attempts = 0;

                do {
                    x = MIN_X + random.nextInt(MAX_X - MIN_X + 1); // 0, 1 или 2
                    y = random.nextInt(MAX_Y + 1); // 0-20
                    coordKey = x + ":" + y;
                    attempts++;

                    // Если долго не можем найти свободное место
                    if (attempts > 100) {
                        // Ищем любое свободное место
                        for (x = MIN_X; x <= MAX_X; x++) {
                            for (y = 0; y <= MAX_Y; y++) {
                                coordKey = x + ":" + y;
                                // Если нашли повторяющиеся координаты выходим
                                if (!usedCoordinates.contains(coordKey)) {
                                    break;
                                }
                            }
                            // Если нашли повторяющиеся координаты выходим
                            if (!usedCoordinates.contains(coordKey)) {
                                break;
                            }
                        }
                        break;
                    }
                } while (usedCoordinates.contains(coordKey));

                usedCoordinates.add(coordKey);

                // Создаем юнита
                Unit newUnit = createUnit(
                        template,
                        unitCounter + 1,
                        x,
                        y);

                // Добавляем в армию
                addUnitSafely(army, newUnit);

                unitCounter++;

            }
        }

        return army;
    }

    private Unit createUnit(Unit template, int number, int x, int y) {
        try {
            return new Unit(
                    template.getUnitType() + " " + number,
                    template.getUnitType(),
                    template.getHealth(),
                    template.getBaseAttack(),
                    template.getCost(),
                    template.getAttackType(),
                    template.getAttackBonuses(),
                    template.getDefenceBonuses(),
                    x,
                    y);
        } catch (Exception e) {
            System.err.println("Ошибка создания юнита: " + e.getMessage());
            return template;
        }
    }

    private void addUnitSafely(Army army, Unit unit) {
        try {
            // Оживляем юнит
            unit.setAlive(true);

            if (army.getUnits() != null) {
                List<Unit> unitsList = army.getUnits();
                unitsList.add(unit);
                army.setUnits(unitsList);
            } else {
                List<Unit> unitsList = new ArrayList<>();
                unitsList.add(unit);

            }
        } catch (Exception e) {
            System.err.println("Ошибка добавления юнита: " + e.getMessage());
        }
    }

    // Класс для хранения ататки здоровья и количества юнитов чтобы в последвтии
    // выбрать из выборки лучгий
    private static class ArmyState {
        long attack;
        long health;
        int[] counts;

        ArmyState(int typeCount) {
            this.attack = 0;
            this.health = 0;
            this.counts = new int[typeCount];
        }

        ArmyState(long attack, long health, int[] counts) {
            this.attack = attack;
            this.health = health;
            this.counts = counts.clone();
        }

        boolean isBetterThan(ArmyState other) {
            // Сначала сравниваем по атаке (приоритет)
            if (this.attack > other.attack)
                return true;
            if (this.attack < other.attack)
                return false;

            // При равной атаке сравниваем по здоровью
            return this.health > other.health;
        }
    }
}