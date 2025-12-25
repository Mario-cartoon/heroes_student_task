package programs;

import com.battle.heroes.army.Unit;
import com.battle.heroes.army.programs.Edge;
import com.battle.heroes.army.programs.UnitTargetPathFinder;
import com.battle.heroes.army.programs.EdgeDistance;

import java.util.*;

/**
 * Метод определяет кратчайший маршрут между атакующим
 * и атакуемым юнитом и возвращает его в виде списка объектов,
 * содержащих координаты каждой точки данного кратчайшего пути.
 * Использовали алгоритмалгоритм поиска по первому наилучшему совпадению на
 * графе,
 * который находит маршрут с наименьшей стоимостью от одной вершины (начальной)
 * к другой (целевой, конечной).
 */
public class UnitTargetPathFinderImpl implements UnitTargetPathFinder {

    @Override
    public List<Edge> getTargetPath(Unit attackUnit, Unit targetUnit, List<Unit> existingUnitList) {
        System.out.println("=== Поиск пути для атаки ===");

        List<Edge> path = new ArrayList<>();

        if (attackUnit == null || targetUnit == null) {
            System.out.println("Ошибка: один из юнитов равен null");
            return path;
        }

        // Логируем информацию о юнитах
        System.out.println("Атакующий юнит: " + attackUnit);
        System.out.println("Целевой юнит: " + targetUnit);

        try {
            // Получаем координаты атакующего и цели
            int startX = getUnitCoordinate(attackUnit, "xCoordinate", "x");
            int startY = getUnitCoordinate(attackUnit, "yCoordinate", "y");
            int targetX = getUnitCoordinate(targetUnit, "xCoordinate", "x");
            int targetY = getUnitCoordinate(targetUnit, "yCoordinate", "y");

            System.out.println("Старт: (" + startX + ", " + startY + ")");
            System.out.println("Цель: (" + targetX + ", " + targetY + ")");

            // Если координаты совпадают, возвращаем путь из одной точки
            if (startX == targetX && startY == targetY) {
                path.add(new Edge(startX, startY));
                System.out.println("Цель уже достигнута (координаты совпадают)");
                return path;
            }

            // Используем алгоритм A* для поиска пути с учетом препятствий
            path = findPathAStar(startX, startY, targetX, targetY, existingUnitList, attackUnit, targetUnit);

            System.out.println("Найден путь длиной " + path.size() + " шагов");

            return path;

        } catch (Exception e) {
            System.err.println("Критическая ошибка при поиске пути: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Вспомогательный метод для получения координаты юнита
     */
    private int getUnitCoordinate(Unit unit, String primaryFieldName, String secondaryFieldName) {
        try {
            // Пробуем основное имя поля
            java.lang.reflect.Field field = unit.getClass().getDeclaredField(primaryFieldName);
            field.setAccessible(true);
            return field.getInt(unit);
        } catch (NoSuchFieldException e) {
            try {
                // Пробуем альтернативное имя поля
                java.lang.reflect.Field field = unit.getClass().getDeclaredField(secondaryFieldName);
                field.setAccessible(true);
                return field.getInt(unit);
            } catch (Exception e2) {
                // Пробуем методы getX/getY
                try {
                    java.lang.reflect.Method method = unit.getClass().getMethod("getX");
                    return (int) method.invoke(unit);
                } catch (Exception e3) {
                    try {
                        java.lang.reflect.Method method = unit.getClass().getMethod("getY");
                        return (int) method.invoke(unit);
                    } catch (Exception e4) {
                        // Возвращаем значение по умолчанию
                        System.out.println("Не удалось получить координату для юнита, используется значение 0");
                        return 0;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка при получении координаты для юнита, используется значение 0");
            return 0;
        }
    }

    /**
     * Упрощенная версия алгоритма A* для поиска пути
     */
    private List<Edge> findPathAStar(int startX, int startY, int targetX, int targetY,
            List<Unit> obstacles, Unit attackUnit, Unit targetUnit) {
        // Создаём множество препятствий
        Set<Point> obstacleSet = new HashSet<>();
        if (obstacles != null) {
            for (Unit unit : obstacles) {
                if (unit != null && unit != attackUnit && unit != targetUnit) {
                    try {
                        int x = getUnitCoordinate(unit, "xCoordinate", "x");
                        int y = getUnitCoordinate(unit, "yCoordinate", "y");
                        obstacleSet.add(new Point(x, y));
                    } catch (Exception e) {
                        // Игнорируем юнитов с некорректными координатами
                    }
                }
            }
        }

        // Точки начала и цели
        Point start = new Point(startX, startY);
        Point goal = new Point(targetX, targetY);

        // Если цель заблокирована — возвращаем прямой путь
        if (obstacleSet.contains(goal)) {
            return createSimplePath(startX, startY, targetX, targetY);
        }

        // Структуры данных для A*
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<Point, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, 0, heuristic(start, goal));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        // Направления движения (8 направлений с диагоналями)
        int[][] directions = {
                { -1, -1 }, { -1, 0 }, { -1, 1 },
                { 0, -1 }, { 0, 1 },
                { 1, -1 }, { 1, 0 }, { 1, 1 }
        };

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.point.equals(goal)) {
                return reconstructPath(current);
            }

            for (int[] dir : directions) {
                Point neighbor = new Point(current.point.x + dir[0], current.point.y + dir[1]);

                // Пропускаем, если это препятствие
                if (obstacleSet.contains(neighbor)) {
                    continue;
                }

                // Стоимость движения: диагональ = 1.41, прямая = 1.0
                double tentativeGScore = current.gScore +
                        (dir[0] != 0 && dir[1] != 0 ? 1.41 : 1.0);

                Node neighborNode = allNodes.get(neighbor);

                if (neighborNode == null) {
                    neighborNode = new Node(neighbor, Double.MAX_VALUE, Double.MAX_VALUE);
                    allNodes.put(neighbor, neighborNode);
                }

                if (tentativeGScore < neighborNode.gScore) {
                    neighborNode.cameFrom = current;
                    neighborNode.gScore = tentativeGScore;
                    neighborNode.fScore = tentativeGScore + heuristic(neighbor, goal);
                    openSet.remove(neighborNode); // Обновляем приоритет
                    openSet.add(neighborNode);
                }
            }
        }

        // Если путь не найден — возвращаем простой (без препятствий)
        return createSimplePath(startX, startY, targetX, targetY);
    }

    /**
     * Эвристика: расстояние Чебышёва (максимум из разницы координат)
     */
    private double heuristic(Point a, Point b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    /**
     * Восстанавливает путь из цели к началу
     */
    private List<Edge> reconstructPath(Node node) {
        List<Edge> path = new ArrayList<>();
        Node current = node;

        while (current != null) {
            path.add(new Edge(current.point.x, current.point.y));
            current = current.cameFrom;
        }

        Collections.reverse(path);
        return path;
    }

    // Вспомогательные классы

    private static class Point {
        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Point point = (Point) o;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    private static class Node {
        final Point point;
        double gScore; // Стоимость от начала до этой точки
        double fScore; // gScore + эвристика
        Node cameFrom; // Откуда пришли

        Node(Point point, double gScore, double fScore) {
            this.point = point;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    /**
     * Проверяет, свободен ли прямой путь между двумя точками
     */
    private boolean isDirectPathClear(int startX, int startY, int targetX, int targetY, Set<String> obstacles) {
        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);

        // Если точки на одной линии по X или Y
        if (dx == 0) {
            int stepY = startY < targetY ? 1 : -1;
            for (int y = startY + stepY; y != targetY; y += stepY) {
                if (obstacles.contains(startX + "," + y)) {
                    return false;
                }
            }
        } else if (dy == 0) {
            int stepX = startX < targetX ? 1 : -1;
            for (int x = startX + stepX; x != targetX; x += stepX) {
                if (obstacles.contains(x + "," + startY)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Поиск пути с учетом препятствий
     */
    private List<Edge> findPathWithObstacles(int startX, int startY, int targetX, int targetY, Set<String> obstacles) {
        List<Edge> path = new ArrayList<>();

        // Простая стратегия: сначала пытаемся двигаться по диагонали, затем по прямой
        int currentX = startX;
        int currentY = startY;

        path.add(new Edge(currentX, currentY));

        // Определяем направление движения
        int dirX = Integer.compare(targetX, startX);
        int dirY = Integer.compare(targetY, startY);

        // Максимальное количество шагов для предотвращения бесконечного цикла
        int maxSteps = 100;
        int steps = 0;

        while ((currentX != targetX || currentY != targetY) && steps < maxSteps) {
            steps++;

            // Пробуем двигаться по диагонали
            if (currentX != targetX && currentY != targetY) {
                int nextX = currentX + dirX;
                int nextY = currentY + dirY;

                if (!obstacles.contains(nextX + "," + nextY)) {
                    currentX = nextX;
                    currentY = nextY;
                    path.add(new Edge(currentX, currentY));
                    continue;
                }
            }

            // Если диагональ заблокирована, двигаемся по X или Y
            if (currentX != targetX) {
                int nextX = currentX + dirX;
                if (!obstacles.contains(nextX + "," + currentY)) {
                    currentX = nextX;
                    path.add(new Edge(currentX, currentY));
                    continue;
                }
            }

            if (currentY != targetY) {
                int nextY = currentY + dirY;
                if (!obstacles.contains(currentX + "," + nextY)) {
                    currentY = nextY;
                    path.add(new Edge(currentX, currentY));
                    continue;
                }
            }

            // Если все направления заблокированы, пытаемся обойти
            break;
        }

        return path;
    }

    /**
     * Создает простой путь
     */
    private List<Edge> createSimplePath(int startX, int startY, int targetX, int targetY) {
        List<Edge> path = new ArrayList<>();

        int currentX = startX;
        int currentY = startY;

        path.add(new Edge(currentX, currentY));

        // Определяем направление движения
        int dirX = Integer.compare(targetX, currentX);
        int dirY = Integer.compare(targetY, currentY);

        // Двигаемся по диагонали, пока можно
        while (currentX != targetX && currentY != targetY) {
            currentX += dirX;
            currentY += dirY;
            path.add(new Edge(currentX, currentY));
        }

        // Двигаемся по оставшейся оси (X или Y)
        while (currentX != targetX) {
            currentX += dirX;
            path.add(new Edge(currentX, currentY));
        }

        while (currentY != targetY) {
            currentY += dirY;
            path.add(new Edge(currentX, currentY));
        }

        return path;
    }

    /**
     * Дополнительный метод для использования EdgeDistance
     */
    public List<EdgeDistance> getTargetPathWithDistance(Unit attackUnit, Unit targetUnit, List<Unit> existingUnitList) {
        List<Edge> path = getTargetPath(attackUnit, targetUnit, existingUnitList);
        List<EdgeDistance> pathWithDistance = new ArrayList<>();

        for (int i = 0; i < path.size(); i++) {
            Edge edge = path.get(i);
            pathWithDistance.add(new EdgeDistance(edge.getX(), edge.getY(), i));
        }

        return pathWithDistance;
    }

    /**
     * Упрощенная версия для отладки - возвращает путь без учета препятствий
     */
    public List<Edge> getSimplePath(Unit attackUnit, Unit targetUnit) {
        try {
            int startX = getUnitCoordinate(attackUnit, "xCoordinate", "x");
            int startY = getUnitCoordinate(attackUnit, "yCoordinate", "y");
            int targetX = getUnitCoordinate(targetUnit, "xCoordinate", "x");
            int targetY = getUnitCoordinate(targetUnit, "yCoordinate", "y");

            return createSimplePath(startX, startY, targetX, targetY);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}