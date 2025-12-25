package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.SuitableForAttackUnitsFinder;

import java.util.*;

/**
 * Метод определяет список юнитов, подходящих для атаки, для атакующего юнита
 * одной из армий.
 * Цель метода — исключить ненужные попытки найти кратчайший путь между юнитами,
 * которые не могут атаковать друг друга.
 */
public class SuitableForAttackUnitsFinderImpl implements SuitableForAttackUnitsFinder {

    @Override
    public List<Unit> getSuitableUnits(List<List<Unit>> unitsByRow, boolean isLeftArmyTarget) {
        if (unitsByRow == null) {
            return Collections.emptyList();
        }

        List<Unit> result = new ArrayList<>();

        for (List<Unit> row : unitsByRow) {
            if (row == null || row.isEmpty()) {
                continue;
            }

            // Сортируем ряд по координате
            List<Unit> sortedRow = sortRowByCoordinate(row, isLeftArmyTarget);

            if (isLeftArmyTarget) {
                addLeftmostUnits(sortedRow, result);
            } else {
                addRightmostUnits(sortedRow, result);
            }
        }

        return result;
    }

    /**
     * Сортирует ряд юнитов по X-координате (или другому критерию)
     */
    private List<Unit> sortRowByCoordinate(List<Unit> row, boolean isLeftArmyTarget) {
        List<Unit> sorted = new ArrayList<>(row);

        // Сортируем по X (предполагаем, что у Unit есть getxCoordinate())
        sorted.sort(Comparator.comparingInt(Unit::getxCoordinate));

        return sorted;
    }

    /**
     * Добавляет самых левых юнитов (с минимальным X)
     */
    private void addLeftmostUnits(List<Unit> sortedRow, List<Unit> result) {
        if (sortedRow.isEmpty())
            return;

        int minX = sortedRow.get(0).getxCoordinate();

        // Добавляем всех юнитов с минимальным X
        for (Unit unit : sortedRow) {
            if (unit.getxCoordinate() == minX) {
                result.add(unit);
            } else {
                break; // Так как список отсортирован
            }
        }
    }

    /**
     * Добавляет самых правых юнитов (с максимальным X)
     */
    private void addRightmostUnits(List<Unit> sortedRow, List<Unit> result) {
        if (sortedRow.isEmpty())
            return;

        int maxX = sortedRow.get(sortedRow.size() - 1).getxCoordinate();

        // Добавляем всех юнитов с максимальным X (идём с конца)
        for (int i = sortedRow.size() - 1; i >= 0; i--) {
            Unit unit = sortedRow.get(i);
            if (unit.getxCoordinate() == maxX) {
                result.add(unit);
            } else {
                break;
            }
        }
    }
}