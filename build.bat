@echo off
echo ===========================================
echo СБОРКА HEROES STUDENT TASK
echo ===========================================

echo 1. Очистка предыдущей сборки...
if exist bin rmdir /s /q bin
mkdir bin

echo 2. Проверка библиотек в libs...
if not exist "libs\*.jar" (
    echo ОШИБКА: Нет библиотек в папке libs!
    echo Скопируйте все JAR файлы из игры в папку libs
    pause
    exit /b 1
)

echo Найдены библиотеки:
dir libs\*.jar

echo 3. Компиляция исходников...
javac -cp "libs\*" -d bin -Xlint:unchecked src\programs\*.java

if %errorlevel% neq 0 (
    echo ОШИБКА КОМПИЛЯЦИИ!
    pause
    exit /b 1
)

echo 4. Создание JAR файла...
cd bin
jar cf ..\heroes_student_task.jar programs\*
cd ..

echo 5. Проверка содержимого JAR...
jar tf heroes_student_task.jar

echo 6. Копирование в папку игры...
set GAME_JARS="C:\projects\heroes\jars"
if not exist %GAME_JARS% (
    echo ОШИБКА: Папка игры не найдена!
    echo Проверьте путь: %GAME_JARS%
    pause
    exit /b 1
)

echo Удаляем старый JAR если есть...
if exist %GAME_JARS%\heroes_student_task.jar del %GAME_JARS%\heroes_student_task.jar

echo Копируем новый JAR...
copy heroes_student_task.jar %GAME_JARS%\

echo 7. Проверка...
echo Файлы в папке игры:
dir %GAME_JARS%\heroes_student_task.jar

echo ===========================================
echo СБОРКА ЗАВЕРШЕНА УСПЕШНО!
echo ===========================================
echo JAR файл создан: heroes_student_task.jar
echo JAR файл скопирован в: %GAME_JARS%\
echo ===========================================
pause